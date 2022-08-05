package com.codingwithmitch.giffit

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import com.codingwithmitch.giffit.MainLoadingState.*
import com.codingwithmitch.giffit.MainViewModel.MainState.*
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorageInteractor.Companion.SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

class MainViewModel
constructor(
    private val cacheProvider: CacheProvider,
    private val versionProvider: VersionProvider
): ViewModel() {

    private val ioScope = CoroutineScope(IO)

    private val captureBitmapsInteractor = CaptureBitmapsInteractor()
    private val saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(versionProvider)
    private val buildGifInteractor = BuildGifInteractor(cacheProvider, versionProvider)
    private val getAssetSizeInteractor = GetAssetSizeInteractor()
    private val resizeGifInteractor = ResizeGifInteractor(
        versionProvider = versionProvider,
        cacheProvider = cacheProvider
    )
    private val clearGifCacheInteractor = ClearGifCacheInteractor(cacheProvider)

    sealed class MainState {

        abstract val capturedBitmaps: List<Bitmap>

        object Initial: MainState() {
            override val capturedBitmaps: List<Bitmap>
                get() = listOf()
        }

        data class DisplaySelectBackgroundAsset(
            val backgroundAssetPickerLauncher: ActivityResultLauncher<String>
        ): MainState() {
            override val capturedBitmaps: List<Bitmap>
                get() = listOf()
        }

        data class DisplayGif(
            val gifUri: Uri?,
            val resizedGifUri: Uri?,
            val originalGifSize: Int,
            val adjustedBytes: Int,
            val sizePercentage: Int,
            val backgroundAssetUri: Uri,
            override val capturedBitmaps: List<Bitmap> = listOf(),
        ): MainState()

        data class DisplayBackgroundAsset(
            val backgroundAssetUri: Uri,
            val capturingViewBounds: Rect? = null,
            override val capturedBitmaps: List<Bitmap> = listOf(),
        ): MainState() {
            val assetData: AssetData = AssetData(
                id = R.drawable.deal_with_it_sunglasses_default,
                initialOffset = Offset(0f, 0f),
                initialSize = Size(200f, 200f)
            )
        }
    }

    val state: MutableState<MainState> = mutableStateOf(Initial)

    val mainLoadingState: MutableState<MainLoadingState> = mutableStateOf(Standard(Idle))
    val error: MutableState<String?> = mutableStateOf(null)
    private val _toastEventRelay: MutableStateFlow<String?> = MutableStateFlow(null)
    val toastEventRelay: Flow<String?> get() = _toastEventRelay

    fun showToast(message: String) {
        _toastEventRelay.tryEmit(message)
    }

    private fun clearCachedFiles() {
        clearGifCacheInteractor.execute().onEach { _ ->
            // Don't update UI here at all. Should just succeed or fail silently.
        }.launchIn(ioScope)
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
        }.launchIn(ioScope).invokeOnCompletion {
            // Whether or not this succeeds we want to clear the cache.
            // Because if something goes wrong we want to reset anyway.
            clearCachedFiles()

            // reset state to displaying the selected background asset.
            state.value = DisplayBackgroundAsset(
                backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri,
            )
        }
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
            }.launchIn(scope)
        }
    }

//    private fun getGifSize(
//        contentResolver: ContentResolver,
//        uri: Uri?,
//    ) {
//        check(state.value is DisplayGif) { "getGifSize: Invalid state: ${state.value}" }
//        getAssetSizeInteractor.execute(
//            contentResolver = contentResolver,
//            uri = uri,
//        ).onEach { dataState ->
//            when(dataState) {
//                is DataState.Data -> {
//                    state.value = (state.value as DisplayGif).copy(
//                        originalGifSize = dataState.data ?: 0,
//                        adjustedBytes = dataState.data ?: 0
//                    )
//                }
//                is DataState.Loading -> {
//                    mainLoadingState.value = Standard(dataState.loadingState)
//                }
//                is DataState.Error -> {
//                    error.value = dataState.message
//                }
//            }
//        }.launchIn(ioScope)
//    }

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
       }.launchIn(ioScope + bitmapCaptureJob).invokeOnCompletion { throwable ->
           val onSuccess: () -> Unit = {
               buildGif(contentResolver = contentResolver)
           }
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
        mainLoadingState.value = Standard(Active())
        buildGifInteractor.execute(
            contentResolver = contentResolver,
            bitmaps = (state.value as DisplayBackgroundAsset).capturedBitmaps,
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
        }.launchIn(ioScope).invokeOnCompletion {
            mainLoadingState.value = Standard(Idle)
        }
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
        check(state.value is DisplayGif) { "updateAdjustedBytes: Invalid state: ${state.value}" }
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










