package com.codingwithmitch.giffit.interactors

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Interactor for capturing a list of bitmaps by screenshotting the device every [CAPTURE_INTERVAL_MS].
 *
 * The way we capture a screenshot diverges for [Build.VERSION_CODES] >= [Build.VERSION_CODES.N].
 * We must use [PixelCopy] for API level 24 (N) and above.
 * This makes things a little annoying because [PixelCopy.request] has a callback we need to use.
 */
class CaptureBitmapsInteractor {

    private sealed class PixelCopyJobState {
        data class Done(
            val bitmap: Bitmap
        ): PixelCopyJobState()

        data class Error(
            val message: String
        ): PixelCopyJobState()
    }

    fun execute(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
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
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pixelCopyJobState = captureBitmap(
                        rect = capturingViewBounds,
                        view = view,
                        window = window,
                    )
                    when (pixelCopyJobState) {
                        is PixelCopyJobState.Done -> {
                            pixelCopyJobState.bitmap
                        }
                        is PixelCopyJobState.Error -> {
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
            Log.e(TAG, "GetAssetSize: ", e)
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(Loading(Idle))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun captureBitmap(
        rect: Rect?,
        window: Window,
        view: View,
    ): PixelCopyJobState = suspendCancellableCoroutine { cont ->
        try {
            check(rect != null) { "Invalid capture area." }
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
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.width.roundToInt(),
                            rect.height.roundToInt()
                        )
                        cont.resume(PixelCopyJobState.Done(bmp))
                    } else {
                        cont.resume(PixelCopyJobState.Error(CAPTURE_BITMAP_ERROR))
                    }
                },
                Handler(Looper.getMainLooper()) )
        } catch (e: Exception) {
            cont.resume(PixelCopyJobState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
    }

    private fun captureBitmap(
        rect: Rect?,
        view: View,
    ): Bitmap {
        check(rect != null) { "Invalid capture area." }
        val bitmap = Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
        return bitmap
    }

    companion object {
        const val TOTAL_CAPTURE_TIME_MS = 4000f
        const val CAPTURE_INTERVAL_MS = 250f
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps."
        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }
}