package com.codingwithmitch.giffit

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import com.codingwithmitch.giffit.BitmapCaptureJobState.Idle
import com.codingwithmitch.giffit.BitmapCaptureJobState.Running
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {

    val bitmapCaptureJobState: MutableState<BitmapCaptureJobState> = mutableStateOf(Idle)
    val capturedBitmaps: MutableState<List<Bitmap>> = mutableStateOf(listOf())

    /**
     * @param sizePercentage: 0.05..1
     */
    fun startBitmapCaptureJob(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
    ) {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            runBitmapCaptureJob(
                capturingViewBounds = capturingViewBounds,
                window = window,
                view = view,
                sizePercentage = sizePercentage,
                onRecordingComplete = onRecordingComplete
            )
        } else {
            runBitmapCaptureJob(
                capturingViewBounds = capturingViewBounds,
                view = view,
                sizePercentage = sizePercentage,
                onRecordingComplete = onRecordingComplete
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runBitmapCaptureJob(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
    ) {
        if (sizePercentage < 0.05 || sizePercentage > 1) throw Exception("Invalid resizing percentage.")
        if (capturingViewBounds == null) throw Exception("Invalid capture area.")
        if (view == null) throw Exception("Invalid view.")
        CoroutineScope(Dispatchers.IO).launch {
            var elapsedTime = 0
            while (elapsedTime < 4000 && bitmapCaptureJobState.value == Running) {
                delay(250)
                elapsedTime += 250
                captureBitmap(
                    rect = capturingViewBounds,
                    view = view,
                    window = window,
                    sizePercentage = sizePercentage,
                ) {
                    // Prevent Concurrent modification exception that can happen depending on timing
                    if (bitmapCaptureJobState.value == Running) {
                        val updated = capturedBitmaps.value.toMutableList()
                        updated.add(it)
                        capturedBitmaps.value = updated
                    }
                }
            }
            onRecordingComplete()
        }
    }

    private fun runBitmapCaptureJob(
        capturingViewBounds: Rect?,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
    ) {
        if (sizePercentage < 0.05 || sizePercentage > 1) throw Exception("Invalid resizing percentage.")
        if (capturingViewBounds == null) throw Exception("Invalid capture area.")
        if (view == null) throw Exception("Invalid view.")
        CoroutineScope(Dispatchers.IO).launch {
            var elapsedTime = 0
            while (elapsedTime < 4000 && bitmapCaptureJobState.value == Running) {
                delay(250)
                elapsedTime += 250
                captureBitmap(
                    rect = capturingViewBounds,
                    view = view,
                    sizePercentage = sizePercentage,
                ) {
                    // Prevent Concurrent modification exception that can happen depending on timing
                    if (bitmapCaptureJobState.value == Running) {
                        val updated = capturedBitmaps.value.toMutableList()
                        updated.add(it)
                        capturedBitmaps.value = updated
                    }
                }
            }
            onRecordingComplete()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, sizePercentage: Float): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }

    private fun captureBitmap(
        rect: Rect?,
        view: View,
        sizePercentage: Float,
        bitmapCallback: (Bitmap)->Unit
    ) {
        if (rect == null) return
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
        if (rect == null) return
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
}