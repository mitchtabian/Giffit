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
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.interactors.CaptureBitmaps.Companion.CAPTURE_BITMAP_SUCCESS
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel : ViewModel() {

    private val ioScope = CoroutineScope(IO)
    private val captureBitmaps = CaptureBitmaps()
    private val saveGifToStorage = SaveGifToStorage()
    private val buildGif = BuildGif(saveGifToStorage)
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
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
        previousUri: Uri?,
        percentageLoss: Float,
    ) {
        gifUri.value?.let {
            val targetSize = gifSize.value * sizePercentage.value.toFloat() / 100
            // Need to be able to cancel the previous resize job.
            val job = Job()
            resizeGif.execute(
                context = context,
                contentResolver = contentResolver,
                capturedBitmaps = capturedBitmaps.value,
                originalGifSize = gifSize.value.toFloat(),
                targetSize = targetSize,
                previousUri = previousUri,
                percentageLoss = percentageLoss,
                launchPermissionRequest = launchPermissionRequest,
                checkFilePermissions = checkFilePermissions
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
                        when(dataState.data) {
                            is ResizeGif.GifResizeResult.Finished -> {
                                resizedGifUri.value = dataState.data.uri
                            }
                            is ResizeGif.GifResizeResult.Continue -> {
                                // Cancel this job because we're moving to the next resize iteration.
                                job.cancel()
                                // Run it again with new percentage loss
                                resizeGif(
                                    context = context,
                                    contentResolver = contentResolver,
                                    previousUri = dataState.data.uri,
                                    percentageLoss = dataState.data.percentageLoss,
                                    launchPermissionRequest = launchPermissionRequest,
                                    checkFilePermissions = checkFilePermissions
                                )
                            }
                        }
                    }
                    is DataState.Error -> {
                        error.value = dataState.message
                    }
                }
            }.launchIn(ioScope + job)
        }
    }

    private fun getGifSize(
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
       contentResolver: ContentResolver,
       capturingViewBounds: Rect?,
       window: Window,
       view: View?,
       launchPermissionRequest: () -> Unit,
       checkFilePermissions: () -> Boolean,
   ) {
       bitmapCaptureJobState.value = Running
       // We need a way to stop the job if a user presses "STOP". So create a Job for this.
       val bitmapCaptureJob = Job()
       val capturedBitmaps: MutableList<Bitmap> = mutableListOf()
       captureBitmaps.execute(
           capturingViewBounds = capturingViewBounds,
           window = window,
           view = view,
       ).onEach { dataState ->
           when(dataState) {
               is DataState.Data -> {
                   // If the user hits the "STOP" button, complete the job by canceling.
                   if (bitmapCaptureJobState.value != Running) {
                       bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
                   }
                   dataState.data?.let { bitmap ->
                       capturedBitmaps.add(bitmap)
                   }
               }
               is DataState.Error -> {
                   // For this use-case, if an error occurs we need to stop the job.
                   // Otherwise it will keep trying to capture bitmaps and failing over and over.
                   bitmapCaptureJob.cancel(CaptureBitmaps.CAPTURE_BITMAP_ERROR)
                   bitmapCaptureJobState.value = Idle
                   error.value = dataState.message
               }
               // TODO("Show determinate progress bar representing how far I am to completion?")
//               is DataState.Loading -> {
//                   loadingState.value = dataState.loadingState
//               }
           }
       }.launchIn(ioScope + bitmapCaptureJob).invokeOnCompletion { throwable ->
           val onSuccess: () -> Unit = {
               // If an error occurs, do not try to build the gif.
               this.capturedBitmaps.value = capturedBitmaps
               buildGif(
                   contentResolver = contentResolver,
                   launchPermissionRequest = launchPermissionRequest,
                   checkFilePermissions = checkFilePermissions
               )
           }
           when (throwable) {
               null -> onSuccess()
               else -> {
                   if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                       onSuccess()
                   } else {
                       Log.d(TAG, "BitmapCaptureJob: throwable: ${throwable.message}")
                       // TODO("Tell the user to select a different image and try again?")
                   }
               }
           }
       }
   }

    private fun buildGif(
        contentResolver: ContentResolver,
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
    ) {
        isBuildingGif.value = true
        buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = capturedBitmaps.value,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
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
                is DataState.Loading -> {
                    loadingState.value = dataState.loadingState
                }
            }
        }.launchIn(ioScope).invokeOnCompletion {
            isBuildingGif.value = false
        }
    }
}










