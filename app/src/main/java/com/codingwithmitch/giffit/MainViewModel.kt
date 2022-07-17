package com.codingwithmitch.giffit

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithmitch.giffit.BitmapCaptureJobState.Idle
import com.codingwithmitch.giffit.BitmapCaptureJobState.Running
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.BuildGif
import com.codingwithmitch.giffit.interactors.CaptureBitmaps
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {

    private val ioScope = CoroutineScope(IO)
    private val captureBitmaps = CaptureBitmaps()
    private val buildGif = BuildGif()

    val bitmapCaptureJobState: MutableState<BitmapCaptureJobState> = mutableStateOf(Idle)
    val capturedBitmaps: MutableState<List<Bitmap>> = mutableStateOf(listOf())
    val loadingState: MutableState<LoadingState> = mutableStateOf(IDLE)
    val error: MutableState<String?> = mutableStateOf(null)
    val isBuildingGif = mutableStateOf(false)

   fun runBitmapCaptureJob(
       capturingViewBounds: Rect?,
       window: Window,
       view: View?,
       sizePercentage: Float,
       onRecordingComplete: () -> Unit,
   ) {
       Log.d(TAG, "runBitmapCaptureJob: called")
       val bitmapCaptureJob = Job()
       val capturedBitmaps: MutableList<Bitmap> = mutableListOf()
       captureBitmaps.execute(
           capturingViewBounds = capturingViewBounds,
           window = window,
           view = view,
           sizePercentage = sizePercentage,
           onRecordingComplete = onRecordingComplete,
           addBitmap = {
               if (bitmapCaptureJobState.value != Running) {
                   Log.d(TAG, "runBitmapCaptureJob: CANCELING")
                   bitmapCaptureJob.cancel()
               } else {
                   Log.d(TAG, "Add bitmap called")
                   capturedBitmaps.add(it)
               }
           }
       ).onEach { dataState ->
           Log.d(TAG, "bitmapCaptureJobState: State: ${bitmapCaptureJobState.value }")
           when(dataState) {
               is DataState.Error -> {
                   error.value = dataState.message
               }
//               is DataState.Loading -> {
//                   loadingState.value = dataState.loadingState
//               }
           }
       }.launchIn(ioScope + bitmapCaptureJob).invokeOnCompletion {
           this.capturedBitmaps.value = capturedBitmaps
           onRecordingComplete()
       }
   }

    fun buildGif(
        context: Context,
        onSaved: (Uri) -> Unit,
        launchPermissionRequest: () -> Unit
    ) {
        isBuildingGif.value = true
        buildGif.execute(
            context = context,
            bitmaps = capturedBitmaps.value,
            onSaved = onSaved,
            launchPermissionRequest = launchPermissionRequest
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Error -> {
                    error.value = dataState.message
                }
//                is DataState.Loading -> {
//                    loadingState.value = dataState.loadingState
//                }
            }
        }.launchIn(ioScope).invokeOnCompletion {
            resetGifJob()
        }
    }

    private fun resetGifJob() {
        isBuildingGif.value = false
        capturedBitmaps.value = listOf()
    }
}










