package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import com.codingwithmitch.giffit.di.Main
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

interface CaptureBitmaps {

    /**
     * @param window is only required if [Build.VERSION_CODES] >= [Build.VERSION_CODES.O].
     *  Otherwise this can be null.
     */
    fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?,
    ): Flow<DataState<List<Bitmap>>>
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class CaptureBitmapsModule {
    @Binds
    abstract fun provideCaptureBitmaps(
        captureBitmaps: CaptureBitmapsInteractor
    ): CaptureBitmaps
}

/**
 * Interactor for capturing a list of bitmaps by screenshotting the device every [CAPTURE_INTERVAL_MS].
 *
 * The way we capture a screenshot diverges for [Build.VERSION_CODES] >= [Build.VERSION_CODES.O].
 * We must use [PixelCopy] for API level 26 (O) and above.
 * This makes things a little annoying because [PixelCopy.request] has a callback we need to use.
 */
class CaptureBitmapsInteractor
@Inject
constructor(
    private val pixelCopyJob: PixelCopyJob,
    private val versionProvider: VersionProvider,
    @Main private val mainDispatcher: CoroutineDispatcher,
): CaptureBitmaps {

    /**
     * Suppress warning since we're using [VersionProvider].
     */
    @SuppressLint("NewApi")
    override fun execute(
        capturingViewBounds: Rect?,
        view: View?,
        window: Window?,
    ): Flow<DataState<List<Bitmap>>> = flow {
        emit(Loading(Active()))
        try {
            check(capturingViewBounds != null) { "Invalid view bounds." }
            check(view != null) { "Invalid view." }
            var elapsedTime = 0f
            val bitmaps: MutableList<Bitmap> = mutableListOf()
            while (elapsedTime < TOTAL_CAPTURE_TIME_MS) {
                delay(CAPTURE_INTERVAL_MS.toLong())
                elapsedTime += CAPTURE_INTERVAL_MS
                emit(Loading(Active(elapsedTime / TOTAL_CAPTURE_TIME_MS)))
                val bitmap = if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                    check(window != null) { "Window is required for PixelCopy." }
                    val pixelCopyJobState = pixelCopyJob.execute(
                        capturingViewBounds = capturingViewBounds,
                        view = view,
                        window = window
                    )
                    when (pixelCopyJobState) {
                        is PixelCopyJob.PixelCopyJobState.Done -> {
                            pixelCopyJobState.bitmap
                        }
                        is PixelCopyJob.PixelCopyJobState.Error -> {
                            throw Exception(pixelCopyJobState.message)
                        }
                    }
                } else {
                    captureBitmap(
                        rect = capturingViewBounds,
                        view = view,
                    )
                }
                // Every time a new bitmap is captured, emit the updated list.
                bitmaps.add(bitmap)
                emit(DataState.Data(bitmaps.toList()))
            }
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(Loading(Idle))
    }

    /**
     * Capture a screenshot on API < [Build.VERSION_CODES.O].
     */
    private suspend fun captureBitmap(
        rect: Rect?,
        view: View,
    ) = withContext(mainDispatcher) {
        check(rect != null) { "Invalid capture area." }
        val bitmap = Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
        return@withContext bitmap
    }

    companion object {
        const val TOTAL_CAPTURE_TIME_MS = 4000f
        const val CAPTURE_INTERVAL_MS = 250f
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps."
        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }
}











