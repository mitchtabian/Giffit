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
import com.canhub.cropper.CropImageView
import com.codingwithmitch.giffit.BitmapCaptureJobState.Idle
import com.codingwithmitch.giffit.BitmapCaptureJobState.Running
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.BuildGif
import com.codingwithmitch.giffit.interactors.CaptureBitmaps
import com.codingwithmitch.giffit.interactors.GetAssetSize
import com.codingwithmitch.giffit.interactors.GetCroppedUriAndSize
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel : ViewModel() {

    private val ioScope = CoroutineScope(IO)
    private val captureBitmaps = CaptureBitmaps()
    private val buildGif = BuildGif()
    private val getAssetSize = GetAssetSize()
    private val getCroppedUriAndSize = GetCroppedUriAndSize()

    val bitmapCaptureJobState: MutableState<BitmapCaptureJobState> = mutableStateOf(Idle)
    val capturedBitmaps: MutableState<List<Bitmap>> = mutableStateOf(listOf())
    val loadingState: MutableState<LoadingState> = mutableStateOf(IDLE)
    val error: MutableState<String?> = mutableStateOf(null)
    val isBuildingGif = mutableStateOf(false)
    val backgroundAsset: MutableState<Uri?> = mutableStateOf(null)
    val uncroppedImageSize: MutableState<Int> = mutableStateOf(0)
    val croppedImageSize: MutableState<Int> = mutableStateOf(0)
    val capturingViewBounds = mutableStateOf<Rect?>(null)
    val assetData = mutableStateOf(
        AssetData(
            id = R.drawable.deal_with_it_sunglasses_default,
            initialOffset = Offset(0f, 0f),
            initialSize = Size(200f, 200f)
        )
    )

    fun getCroppedUriAndSize(
        result: CropImageView.CropResult
    ) {
        getCroppedUriAndSize.execute(
            result = result,
            uncroppedImageSize = uncroppedImageSize.value
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    croppedImageSize.value = dataState.data?.size ?: 0
                    Log.d(TAG, "croppedImageSize: ${croppedImageSize.value}")
                    backgroundAsset.value = dataState.data?.uri
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
            }
        }.launchIn(ioScope)
    }

    fun getUncroppedBackgroundAssetSize(
        contentResolver: ContentResolver,
        uncroppedBackgroundAssetUri: Uri?,
        onComplete: () -> Unit,
    ) {
        getAssetSize.execute(
            contentResolver = contentResolver,
            uri = uncroppedBackgroundAssetUri,
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    uncroppedImageSize.value = dataState.data ?: 0
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
            }
        }.launchIn(ioScope).invokeOnCompletion {
            if (it == null) {
                onComplete()
            }
        }
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










