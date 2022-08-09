package com.codingwithmitch.giffit

import android.content.ContentResolver
import android.net.Uri
import android.view.View
import android.view.Window
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithmitch.giffit.MainLoadingState.*
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.di.IO
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorageInteractor.Companion.SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    @IO private val ioDispatcher: CoroutineDispatcher,
    private val captureBitmapsInteractor: CaptureBitmapsInteractor,
    private val saveGifToExternalStorageInteractor: SaveGifToExternalStorageInteractor,
    private val buildGifInteractor: BuildGifInteractor,
    private val resizeGifInteractor: ResizeGifInteractor,
    private val clearGifCacheInteractor: ClearGifCacheInteractor,
): ViewModel() {

    val state: MutableState<MainState> = mutableStateOf(Initial)
    val mainLoadingState: MutableState<MainLoadingState> = mutableStateOf(Standard(Idle))
    val error: MutableState<String?> = mutableStateOf(null)
    private val _toastEventRelay: MutableStateFlow<String?> = MutableStateFlow(null)
    val toastEventRelay: Flow<String?> get() = _toastEventRelay

    private fun showToast(message: String) {
        _toastEventRelay.tryEmit(message)
    }

    private fun clearCachedFiles() {
        clearGifCacheInteractor.execute().onEach { _ ->
            // Don't update UI here. Should just succeed or fail silently.
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun saveGif(
        contentResolver: ContentResolver,
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
    ) {
        check(state.value is DisplayGif) { "saveGif: Invalid state: ${state.value}" }
        val uriToSave = (state.value as DisplayGif).let {
            it.resizedGifUri ?: it.gifUri
        } ?: throw Exception(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR)
        saveGifToExternalStorageInteractor.execute(
            contentResolver = contentResolver,
            cachedUri = uriToSave,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    showToast("Saved")
                }
                is DataState.Loading -> {
                    mainLoadingState.value = Standard(dataState.loadingState)
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
            }
        }.onCompletion {
            mainLoadingState.value = Standard(Idle)
            // Whether or not this succeeds we want to clear the cache.
            // Because if something goes wrong we want to reset anyway.
            clearCachedFiles()

            // reset state to displaying the selected background asset.
            state.value = DisplayBackgroundAsset(
                backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri,
            )
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun resizeGif(
        contentResolver: ContentResolver,
    ) {
        check(state.value is DisplayGif) { "resizeGif: Invalid state: ${state.value}" }
        (state.value as DisplayGif).let {
            // Calculate the target size of the resulting gif.
            val targetSize = it.originalGifSize * it.sizePercentage.toFloat() / 100
            // Need to be able to cancel the previous resize job.
            val job = Job()
            val scope = CoroutineScope(IO + job)
            resizeGifInteractor.execute(
                contentResolver = contentResolver,
                capturedBitmaps = it.capturedBitmaps,
                originalGifSize = it.originalGifSize.toFloat(),
                targetSize = targetSize,
                discardCachedGif = {
                    discardCachedGif(it)
                },
            ).onEach { dataState ->
                when(dataState) {
                    is DataState.Loading -> {
                        mainLoadingState.value = ResizingGif(dataState.loadingState)
                    }
                    is DataState.Data -> {
                        state.value = (state.value as DisplayGif).copy(
                            resizedGifUri = dataState.data
                        )
                    }
                    is DataState.Error -> {
                        error.value = dataState.message
                    }
                }
            }.onCompletion {
                mainLoadingState.value = Standard(Idle)
            }.flowOn(ioDispatcher).launchIn(scope)
        }
    }

   fun runBitmapCaptureJob(
       contentResolver: ContentResolver,
       capturingViewBounds: Rect?,
       window: Window,
       view: View?,
   ) {
       check(state.value is DisplayBackgroundAsset) { "runBitmapCaptureJob: Invalid state: ${state.value}" }
       mainLoadingState.value = BitmapCapture(Active(0f))
       // We need a way to stop the job if a user presses "STOP". So create a Job for this.
       val bitmapCaptureJob = Job()
       // Create convenience function for checking if the user pressed "STOP".
       val checkShouldCancelJob: (MainLoadingState) -> Unit = { loadingState ->
           if (loadingState is BitmapCapture) {
               if (loadingState.loadingState !is Active) {
                   bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
               }
           }
       }
       // Execute the use-case.
       captureBitmapsInteractor.execute(
           capturingViewBounds = capturingViewBounds,
           window = window,
           view = view,
       ).onEach { dataState ->
           // If the user hits the "STOP" button, complete the job by canceling.
           checkShouldCancelJob(mainLoadingState.value)
           when(dataState) {
               is DataState.Data -> {
                   dataState.data?.let { bitmaps ->
                       state.value = (state.value as DisplayBackgroundAsset).copy(
                           capturedBitmaps = bitmaps
                       )
                   }
               }
               is DataState.Error -> {
                   // For this use-case, if an error occurs we need to stop the job.
                   // Otherwise it will keep trying to capture bitmaps and failing over and over.
                   bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)
                   mainLoadingState.value = None
                   error.value = dataState.message
               }
               is DataState.Loading -> {
                   mainLoadingState.value = BitmapCapture(dataState.loadingState)
               }
           }
       }.flowOn(ioDispatcher).launchIn(viewModelScope + bitmapCaptureJob).invokeOnCompletion { throwable ->
           mainLoadingState.value = Standard(Idle)
           val onSuccess: () -> Unit = {
               buildGif(contentResolver = contentResolver)
           }
           // If the throwable is null OR the message = CAPTURE_BITMAP_SUCCESS, it was successful.
           when (throwable) {
               null -> onSuccess()
               else -> {
                   if (throwable.message == CAPTURE_BITMAP_SUCCESS) {
                       onSuccess()
                   } else { // If an error occurs, do not try to build the gif.
                       error.value = throwable.message ?: CAPTURE_BITMAP_ERROR
                   }
               }
           }
       }
   }

    private fun buildGif(
        contentResolver: ContentResolver,
    ) {
        check(state.value is DisplayBackgroundAsset) { "buildGif: Invalid state: ${state.value}" }
        val capturedBitmaps = (state.value as DisplayBackgroundAsset).capturedBitmaps
        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to build a gif from!" }
        mainLoadingState.value = Standard(Active())
        buildGifInteractor.execute(
            contentResolver = contentResolver,
            bitmaps = capturedBitmaps,
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    (state.value as DisplayBackgroundAsset).let {
                        val gifSize = dataState.data?.gifSize ?: 0
                        state.value = DisplayGif(
                            gifUri = dataState.data?.uri,
                            resizedGifUri = null,
                            originalGifSize = gifSize,
                            adjustedBytes = gifSize,
                            sizePercentage = 100,
                            backgroundAssetUri = it.backgroundAssetUri,
                            capturedBitmaps = it.capturedBitmaps
                        )
                    }
                }
                is DataState.Error -> {
                    error.value = dataState.message
                }
                is DataState.Loading -> {
                    mainLoadingState.value = Standard(dataState.loadingState)
                }
            }
        }.onCompletion {
            mainLoadingState.value = Standard(Idle)
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun resetGifToOriginal() {
        check(state.value is DisplayGif) { "resetGifToOriginal: Invalid state: ${state.value}" }
        (state.value as DisplayGif).run {
            resizedGifUri?.let {
                discardCachedGif(it)
            }
            state.value = this.copy(
                resizedGifUri = null,
                adjustedBytes = originalGifSize,
                sizePercentage = 100
            )
        }
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state.value is DisplayGif) { "deleteGif: Invalid state: ${state.value}" }
        state.value = DisplayBackgroundAsset(
            backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri,
        )
    }

    fun updateAdjustedBytes(adjustedBytes: Int) {
        check(state.value is DisplayGif) { "updateAdjustedBytes: Invalid state: ${state.value}" }
        state.value = (state.value as DisplayGif).copy(
            adjustedBytes = adjustedBytes
        )
    }

    fun updateSizePercentage(sizePercentage: Int) {
        check(state.value is DisplayGif) { "updateSizePercentage: Invalid state: ${state.value}" }
        state.value = (state.value as DisplayGif).copy(
            sizePercentage = sizePercentage
        )
    }

    companion object {
        const val DISCARD_CACHED_GIF_ERROR = "Failed to delete cached gif at uri: "

        @VisibleForTesting fun discardCachedGif(uri: Uri) {
            val file = File(uri.path)
            val success = file.delete()
            if (!success) {
                throw Exception("$DISCARD_CACHED_GIF_ERROR $uri.")
            }
        }
    }
}










