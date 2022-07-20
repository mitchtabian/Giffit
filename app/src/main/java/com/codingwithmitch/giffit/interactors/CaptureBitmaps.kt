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
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

/**
 * TODO("ktdoc")
 */
class CaptureBitmaps {

    fun execute(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
    ): Flow<DataState<Bitmap>> = flow {
        emit(Loading(Active()))
        try {
            emitAll(
                runBitmapCaptureJob(
                    capturingViewBounds = capturingViewBounds,
                    window = window,
                    view = view,
                )
            )
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: CAPTURE_BITMAP_ERROR))
        }
        emit(Loading(Idle))
    }

    private fun runBitmapCaptureJob(
        capturingViewBounds: Rect?,
        window: Window,
        view: View?,
    ): Flow<DataState<Bitmap>> = flow {
        check(capturingViewBounds != null) { "Invalid view bounds." }
        check(view != null) { "Invalid view." }
        var elapsedTime = 0
        while (elapsedTime < 4000) {
            delay(250)
            elapsedTime += 250
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                emitAll(
                    captureBitmap(
                        rect = capturingViewBounds,
                        view = view,
                        window = window,
                    )
                )
            } else {
                emitAll(
                    captureBitmap(
                        rect = capturingViewBounds,
                        view = view,
                    )
                )
            }
        }
    }

    private fun captureBitmap(
        rect: Rect?,
        view: View,
    ): Flow<DataState<Bitmap>> = flow {
        check(rect != null) { "Invalid capture area." }
        val bitmap = Bitmap.createBitmap(
            rect.width.roundToInt(),
            rect.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-rect.left, -rect.top)
            view.draw(this)
        }
        emit(DataState.Data(bitmap))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureBitmap(
        rect: Rect?,
        window: Window,
        view: View,
    ): Flow<DataState<Bitmap>> = callbackFlow { // Have to use callbackFlow b/c PixelCopy is a fn with callback.
        val completionListener = object: CallbackFlowCompletionListener {
            override fun finished(errorMessage: String?) {
                errorMessage?.let {
                    trySend(DataState.Error<Bitmap>(it))
                }
                close() // Finish the callbackFlow
            }
        }
        // Not sure if this try/catch is necessary. The callbackFlow api doesn't say what will
        // happen if exception is thrown. Not sure if the flow gets canceled so I'll do it manually.
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
                    // While I was testing I found that if an exception is thrown here it is not caught by
                    // the outer try/catch.
                    try {
                        if (p0 == PixelCopy.SUCCESS) {
                            // Crop the screenshot
                            val dataState = DataState.Data(
                                Bitmap.createBitmap(
                                    bitmap,
                                    rect.left.toInt(),
                                    rect.top.toInt(),
                                    rect.width.roundToInt(),
                                    rect.height.roundToInt()
                                )
                            )
                            trySend(dataState)
                            completionListener.finished(null)
                        }
                    } catch (e: Exception) {
                        completionListener.finished(e.message ?: CAPTURE_BITMAP_ERROR)
                    }
                },
                Handler(Looper.getMainLooper()) )
        } catch (e: Exception) {
            completionListener.finished(e.message ?: CAPTURE_BITMAP_ERROR)
        }
        awaitClose { completionListener.finished(null) }
    }

    private interface CallbackFlowCompletionListener {
        fun finished(errorMessage: String?)
    }

    companion object {
        const val CAPTURE_BITMAP_ERROR = "An error occurred while capturing the bitmaps."
        const val CAPTURE_BITMAP_SUCCESS = "Completed Successfully"
    }
}