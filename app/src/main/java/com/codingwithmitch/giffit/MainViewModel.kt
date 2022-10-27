package com.codingwithmitch.giffit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.di.IO
import com.codingwithmitch.giffit.domain.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.codingwithmitch.giffit.interactors.ResizeGifInteractor.Companion.RESIZE_GIF_ERROR
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorageInteractor.Companion.SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    @IO private val ioDispatcher: CoroutineDispatcher,
    private val captureBitmaps: CaptureBitmaps,
    private val saveGifToExternalStorage: SaveGifToExternalStorage,
    private val buildGif: BuildGif,
    private val resizeGif: ResizeGif,
    private val clearGifCache: ClearGifCache,
    private val versionProvider: VersionProvider,
): ViewModel() {

    private val _state: MutableState<MainState> = mutableStateOf(Initial)
    val state: State<MainState> get() = _state
    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay
    private val _errorRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorRelay: StateFlow<Set<ErrorEvent>> get() = _errorRelay

    fun resizeGif(
        contentResolver: ContentResolver,
    ) {
        check(state.value is DisplayGif) { "resizeGif: Invalid state: ${state.value}" }
        (state.value as DisplayGif).let {
            // Calculate the target size of the resulting gif.
            val targetSize = it.originalGifSize * it.sizePercentage.toFloat() / 100

            resizeGif.execute(
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
                        updateState(
                            (state.value as DisplayGif).copy(resizeGifLoadingState = dataState.loadingState)
                        )
                    }
                    is DataState.Data -> {
                        dataState.data?.let { data ->
                            _state.value = (state.value as DisplayGif).copy(
                                resizedGifUri = data.uri,
                                adjustedBytes = data.gifSize
                            )
                        } ?: throw Exception(RESIZE_GIF_ERROR)
                    }
                    is DataState.Error -> {
                        publishErrorEvent(
                            ErrorEvent(
                                id = UUID.randomUUID().toString(),
                                message = dataState.message
                            )
                        )
                        updateState(
                            (state.value as DisplayGif).copy(
                                loadingState = Idle,
                                resizeGifLoadingState = Idle
                            )
                        )
                    }
                }
            }.onCompletion {
                withContext(Main) {
                    updateState(
                        (state.value as DisplayGif).copy(loadingState = Idle)
                    )
                }
            }.flowOn(ioDispatcher).launchIn(viewModelScope)
        }
    }

    fun saveGif(
        contentResolver: ContentResolver,
        context: Context,
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
    ) {
        check(state.value is DisplayGif) { "saveGif: Invalid state: ${state.value}" }
        // Ask permission if necessary
        if (versionProvider.provideVersion() < Build.VERSION_CODES.Q  && !checkFilePermissions()) {
            launchPermissionRequest()
            return
        }
        val uriToSave = (state.value as DisplayGif).let {
            it.resizedGifUri ?: it.gifUri
        } ?: throw Exception(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR)
        saveGifToExternalStorage.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = uriToSave,
            checkFilePermissions = checkFilePermissions
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> showToast(message = "Saved")
                is DataState.Loading -> {
                    updateState(
                        (state.value as DisplayGif).copy(loadingState = dataState.loadingState)
                    )
                }
                is DataState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                    updateState(
                        (state.value as DisplayGif).copy(loadingState = Idle)
                    )
                }
            }
        }.onCompletion {
            // Whether or not this succeeds we want to clear the cache.
            // Because if something goes wrong we want to reset anyway.
            clearCachedFiles()

            withContext(Main) {
                // reset state to displaying the selected background asset.
                updateState(
                    DisplayBackgroundAsset(
                        backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri,
                    )
                )
            }
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    private fun buildGif(
        contentResolver: ContentResolver,
    ) {
        check(state.value is DisplayBackgroundAsset) { "buildGif: Invalid state: ${state.value}" }
        val capturedBitmaps = (state.value as DisplayBackgroundAsset).capturedBitmaps
        check(capturedBitmaps.isNotEmpty()) { "You have no bitmaps to build a gif from!" }
        updateState(
            (state.value as DisplayBackgroundAsset).copy(loadingState = Active())
        )
        buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = capturedBitmaps,
        ).onEach { dataState ->
            when(dataState) {
                is DataState.Data -> {
                    (state.value as DisplayBackgroundAsset).let {
                        val gifSize = dataState.data?.gifSize ?: 0
                        val gifUri = dataState.data?.uri
                        updateState(
                            DisplayGif(
                                gifUri = gifUri,
                                backgroundAssetUri = it.backgroundAssetUri,
                                resizedGifUri = null,
                                originalGifSize = gifSize,
                                adjustedBytes = gifSize,
                                sizePercentage = 100,
                                capturedBitmaps = it.capturedBitmaps
                            )
                        )
                    }
                }
                is DataState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(
                            loadingState = Idle,
                            bitmapCaptureLoadingState = Idle
                        )
                    )
                }
                is DataState.Loading -> {
                    // Need to check here since there is a state change to DisplayGif and loading
                    //  emissions can technically still come after the job is complete
                    if (state.value is DisplayBackgroundAsset) {
                        updateState(
                            (state.value as DisplayBackgroundAsset).copy(
                                loadingState = dataState.loadingState
                            )
                        )
                    }
                }
            }
        }.onCompletion {
            withContext(Main) {
                if (state.value is DisplayBackgroundAsset) {
                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(
                            loadingState = Idle,
                            bitmapCaptureLoadingState = Idle
                        )
                    )
                }
            }
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun runBitmapCaptureJob(
        contentResolver: ContentResolver,
        view: View,
        window: Window
    ) {
        check(state.value is DisplayBackgroundAsset) { "Invalid state: ${state}" }
        updateState((state.value as DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = Active(0f)))

        // We need a way to stop the job if a user presses "STOP". So create a Job for this.
        val bitmapCaptureJob = Job()
        // Create convenience function for checking if the user pressed "STOP".
        val checkShouldCancelJob: (MainState) -> Unit = { mainState ->
            val shouldCancel = when(mainState) {
                is DisplayBackgroundAsset -> {
                    mainState.bitmapCaptureLoadingState !is Active
                }
                else -> true
            }
            if (shouldCancel) {
                bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
            }
        }
        // Execute the use-case.
        captureBitmaps.execute(
            capturingViewBounds = (state.value as DisplayBackgroundAsset).capturingViewBounds,
            window = window,
            view = view,
        ).onEach { dataState ->
            // If the user hits the "STOP" button, complete the job by canceling.
            // Also cancel if there was some kind of state change.
            checkShouldCancelJob(state.value)
            when(dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        updateState((state.value as DisplayBackgroundAsset).copy(capturedBitmaps = bitmaps))
                    }
                }
                is DataState.Error -> {
                    // For this use-case, if an error occurs we need to stop the job.
                    // Otherwise it will keep trying to capture bitmaps and failing over and over.
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)

                    updateState(
                        (state.value as DisplayBackgroundAsset).copy(
                            bitmapCaptureLoadingState = Idle,
                            loadingState = Idle
                        )
                    )
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }
                is DataState.Loading -> {
                    updateState((state.value as DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = dataState.loadingState))
                }
            }
        }.flowOn(ioDispatcher).launchIn(viewModelScope + bitmapCaptureJob).invokeOnCompletion { throwable ->
            updateState(
                (state.value as DisplayBackgroundAsset).copy(
                    bitmapCaptureLoadingState = Idle,
                    loadingState = Idle
                )
            )
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
                        publishErrorEvent(
                            ErrorEvent(
                                id = UUID.randomUUID().toString(),
                                message = throwable.message ?: CAPTURE_BITMAP_ERROR
                            )
                        )
                    }
                }
            }
        }
    }

    private fun clearCachedFiles() {
        clearGifCache.execute().onEach { _ ->
            // Don't update UI here. Should just succeed or fail silently.
        }.flowOn(ioDispatcher).launchIn(viewModelScope)
    }

    fun deleteGif() {
        clearCachedFiles()
        check(state.value is DisplayGif) { "deleteGif: Invalid state: ${state.value}" }
        _state.value = DisplayBackgroundAsset(
            backgroundAssetUri = (state.value as DisplayGif).backgroundAssetUri,
        )
    }

    fun endBitmapCaptureJob() {
        updateState((state.value as DisplayBackgroundAsset).copy(bitmapCaptureLoadingState = Idle))
    }

    fun updateState(mainState: MainState) {
        _state.value = mainState
    }

    fun showToast(
        id: String = UUID.randomUUID().toString(),
        message: String
    ) {
        _toastEventRelay.tryEmit(
            ToastEvent(
                id = id,
                message = message
            )
        )
    }

    private fun publishErrorEvent(errorEvent: ErrorEvent) {
        val current = _errorRelay.value.toMutableSet()
        current.add(errorEvent)
        _errorRelay.value = current
    }

    fun clearErrorEvents() {
        _errorRelay.value = setOf()
    }

    fun resetGifToOriginal() {
        check(state.value is DisplayGif) { "resetGifToOriginal: Invalid state: ${state.value}" }
        (state.value as DisplayGif).run {
            resizedGifUri?.let {
                discardCachedGif(it)
            }
            _state.value = this.copy(
                resizedGifUri = null,
                adjustedBytes = originalGifSize,
                sizePercentage = 100
            )
        }
    }

    fun updateAdjustedBytes(adjustedBytes: Int) {
        check(state.value is DisplayGif) { "updateAdjustedBytes: Invalid state: ${state.value}" }
        _state.value = (state.value as DisplayGif).copy(
            adjustedBytes = adjustedBytes
        )
    }

    fun updateSizePercentage(sizePercentage: Int) {
        check(state.value is DisplayGif) { "updateSizePercentage: Invalid state: ${state.value}" }
        _state.value = (state.value as DisplayGif).copy(
            sizePercentage = sizePercentage
        )
    }

    companion object {
        const val DISCARD_CACHED_GIF_ERROR = "Failed to delete cached gif at uri: "

        /**
         * Added to companion object so it's easily used in Unit tests.
         */
        private fun discardCachedGif(uri: Uri) {
            val file = File(uri.path)
            val success = file.delete()
            if (!success) {
                throw Exception("$DISCARD_CACHED_GIF_ERROR $uri.")
            }
        }
    }
}