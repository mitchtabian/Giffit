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
import androidx.core.graphics.applyCanvas
import com.codingwithmitch.giffit.BitmapUtils.resizeBitmap
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

class CaptureBitmaps {

    class CaptureBitmapsCancelException: Exception()

    fun execute(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
        addBitmap: (Bitmap) -> Unit
    ): Flow<DataState<Nothing>> = flow {
        emit(Loading(LOADING))
        try {
            runBitmapCaptureJob(
                capturingViewBounds = capturingViewBounds,
                window = window,
                view = view,
                sizePercentage = sizePercentage,
                onRecordingComplete = onRecordingComplete
            ) {
                addBitmap(it)
            }
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(Loading(IDLE))
    }

    private suspend fun runBitmapCaptureJob(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
        addBitmap: (Bitmap) -> Unit,
    ) {
        check(sizePercentage in 0.05..1.0) { "Invalid resizing percentage." }
        check(capturingViewBounds != null) { "Invalid resizing percentage." }
        check(view != null) { "Invalid view." }
        var elapsedTime = 0
        while (elapsedTime < 4000) {
            delay(250)
            elapsedTime += 250
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                captureBitmap(
                    rect = capturingViewBounds,
                    view = view,
                    window = window,
                    sizePercentage = sizePercentage,
                    bitmapCallback = addBitmap
                )
            } else {
                captureBitmap(
                    rect = capturingViewBounds,
                    view = view,
                    sizePercentage = sizePercentage,
                    bitmapCallback = addBitmap
                )
            }

        }
        onRecordingComplete()
    }

    private fun captureBitmap(
        rect: Rect?,
        view: View,
        sizePercentage: Float,
        bitmapCallback: (Bitmap)->Unit
    ) {
        check(rect != null) { "Invalid capture area." }
        val bitmap = resizeBitmap(
            bitmap = Bitmap.createBitmap(
                rect.width.roundToInt(),
                rect.height.roundToInt(),
                Bitmap.Config.ARGB_8888
            ),
            sizePercentage = sizePercentage
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
        bitmapCallback.invoke(bitmap)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureBitmap(
        rect: Rect?,
        window: Window,
        view: View,
        sizePercentage: Float,
        bitmapCallback: (Bitmap)->Unit
    ) {
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
                    // crop the screenshot
                    bitmapCallback.invoke(
                        resizeBitmap(
                            bitmap = Bitmap.createBitmap(
                                bitmap,
                                rect.left.toInt(),
                                rect.top.toInt(),
                                rect.width.roundToInt(),
                                rect.height.roundToInt()
                            ),
                            sizePercentage = sizePercentage
                        )
                    )
                }
            },
            Handler(Looper.getMainLooper()) )
    }

    companion object {
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps."
    }
}