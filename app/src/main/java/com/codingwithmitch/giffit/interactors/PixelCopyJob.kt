package com.codingwithmitch.giffit.interactors

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import com.codingwithmitch.giffit.interactors.PixelCopyJob.*
import com.codingwithmitch.giffit.interactors.PixelCopyJob.PixelCopyJobState.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * It might seem unnecessary to do an interface/impl for this use-case but it makes
 *  mocking easier for tests. Mocking the window & view is not simple. This will
 *  make the PixelCopy job simple to test.
 */
interface PixelCopyJob {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun execute(
        capturingViewBounds: Rect?,
        view: View,
        window: Window
    ): PixelCopyJobState

    sealed class PixelCopyJobState {
        data class Done(
            val bitmap: Bitmap
        ): PixelCopyJobState()

        data class Error(
            val message: String
        ): PixelCopyJobState()
    }
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class PixelCopyJobModule {
    @Binds
    abstract fun providePixelCopyJob(
        pixelCopyJob: PixelCopyJobInteractor
    ): PixelCopyJob
}

class PixelCopyJobInteractor
@Inject
constructor(): PixelCopyJob {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun execute(
        capturingViewBounds: Rect?,
        view: View,
        window: Window
    ): PixelCopyJobState = suspendCancellableCoroutine { cont ->
        try {
            check(capturingViewBounds != null) { "Invalid capture area." }
            val bitmap = Bitmap.createBitmap(
                view.width, view.height,
                Bitmap.Config.ARGB_8888
            )

            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            val xCoordinate = locationOfViewInWindow[0]
            val yCoordinate = locationOfViewInWindow[1]
            val scope = android.graphics.Rect(
                xCoordinate,
                yCoordinate,
                xCoordinate + view.width,
                yCoordinate + view.height
            )
            // Take screenshot
            PixelCopy.request(
                window,
                scope,
                bitmap,
                { p0 ->
                    if (p0 == PixelCopy.SUCCESS) {
                        // Crop the screenshot
                        val bmp = Bitmap.createBitmap(
                            bitmap,
                            capturingViewBounds.left.toInt(),
                            capturingViewBounds.top.toInt(),
                            capturingViewBounds.width.roundToInt(),
                            capturingViewBounds.height.roundToInt()
                        )
                        cont.resume(Done(bmp))
                    } else {
                        cont.resume(Error(PIXEL_COPY_ERROR))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            cont.resume(Error(e.message ?: PIXEL_COPY_ERROR))
        }
    }

    companion object {
        const val PIXEL_COPY_ERROR = "An error occurred while running PixelCopy."
    }
}











