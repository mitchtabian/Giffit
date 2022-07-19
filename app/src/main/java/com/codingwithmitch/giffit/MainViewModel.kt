package com.codingwithmitch.giffit

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import com.codingwithmitch.giffit.BitmapCaptureJobState.Idle
import com.codingwithmitch.giffit.BitmapCaptureJobState.Running
import com.codingwithmitch.giffit.BitmapUtils.discardGif
import com.codingwithmitch.giffit.BitmapUtils.resizeBitmap
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel : ViewModel() {

    private val ioScope = CoroutineScope(IO)
    private val captureBitmaps = CaptureBitmaps()
    private val buildGif = BuildGif()
    private val getAssetSize = GetAssetSize()
    private val resizeGif = ResizeGif(
        buildGif = buildGif,
        getAssetSize = getAssetSize,
        ioScope = ioScope
    )

    val bitmapCaptureJobState: MutableState<BitmapCaptureJobState> = mutableStateOf(Idle)
    val capturedBitmaps: MutableState<List<Bitmap>> = mutableStateOf(listOf())
    val loadingState: MutableState<LoadingState> = mutableStateOf(LoadingState.Idle)
    val error: MutableState<String?> = mutableStateOf(null)
    val isBuildingGif = mutableStateOf(false)
    val backgroundAsset: MutableState<Uri?> = mutableStateOf(null)
    val capturingViewBounds = mutableStateOf<Rect?>(null)
    val resizedGifUri: MutableState<Uri?> = mutableStateOf(null)
    val gifUri: MutableState<Uri?> = mutableStateOf(null)
    val gifSize: MutableState<Int> = mutableStateOf(0)
    val adjustedBytes: MutableState<Int> = mutableStateOf(0)
    val sizePercentage: MutableState<Int> = mutableStateOf(100)
    val assetData = mutableStateOf(
        AssetData(
            id = R.drawable.deal_with_it_sunglasses_default,
            initialOffset = Offset(0f, 0f),
            initialSize = Size(200f, 200f)
        )
    )
    val gifResizeProgress: MutableState<Float> = mutableStateOf(0f)

    fun resizeGif(
        context: Context,
        contentResolver: ContentResolver,
        launchPermissionRequest: () -> Unit
    ) {
//        resizeGif.test().onEach { value ->
//            Log.d(TAG, "resizeGif: ${value}")
//        }.launchIn(ioScope)
        gifUri.value?.let {
            isBuildingGif.value = true
            val targetSize = gifSize.value * sizePercentage.value.toFloat() / 100
            resizeGif.execute(
                context = context,
                contentResolver = contentResolver,
                capturedBitmaps = capturedBitmaps.value,
                originalGifSize = gifSize.value.toFloat(),
                targetSize = targetSize,
                launchPermissionRequest = launchPermissionRequest,
            ).onEach { dataState ->
                when(dataState) {
                    is DataState.Loading -> {
                        when(dataState.loadingState) {
                            is Active -> {
                                gifResizeProgress.value = dataState.loadingState.progress ?: 0f
                            }
                            LoadingState.Idle -> {
                                gifResizeProgress.value = 0f
                            }
                        }
                    }
                    is DataState.Data -> {
                        resizedGifUri.value = dataState.data
                    }
                    is DataState.Error -> {
                        error.value = dataState.message
                    }
                }
            }.launchIn(ioScope)
        }
    }

    fun getGifSize(
        contentResolver: ContentResolver,
        uri: Uri?,
    ) {
        getAssetSize.execute(
            contentResolver = contentResolver,
            uri = uri,
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    gifSize.value = dataState.data ?: 0
                    adjustedBytes.value = dataState.data ?: 0
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
            }
        }.launchIn(ioScope)
    }

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
        contentResolver: ContentResolver,
        launchPermissionRequest: () -> Unit
    ) {
        isBuildingGif.value = true
        buildGif.execute(
            context = context,
            bitmaps = capturedBitmaps.value,
            launchPermissionRequest = launchPermissionRequest
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    gifUri.value = dataState.data
                    getGifSize(
                        contentResolver = contentResolver,
                        uri = dataState.data
                    )
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
//                is DataState.Loading -> {
//                    loadingState.value = dataState.loadingState
//                }
            }
        }.launchIn(ioScope).invokeOnCompletion {
            isBuildingGif.value = false
        }
    }

//    fun buildGif(
//        context: Context,
//        onSaved: (Uri) -> Unit,
//        launchPermissionRequest: () -> Unit
//    ) {
//        isBuildingGif.value = true
//        buildGif.execute(
//            context = context,
//            bitmaps = capturedBitmaps.value,
//            onSaved = onSaved,
//            launchPermissionRequest = launchPermissionRequest
//        ).onEach { dataState ->
//            when(dataState) {
//                is DataState.Error -> {
//                    error.value = dataState.message
//                }
////                is DataState.Loading -> {
////                    loadingState.value = dataState.loadingState
////                }
//            }
//        }.launchIn(ioScope).invokeOnCompletion {
//            isBuildingGif.value = false
//        }
//    }
}










