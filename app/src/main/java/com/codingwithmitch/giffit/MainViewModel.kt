package com.codingwithmitch.giffit

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.interactors.CaptureBitmaps
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_ERROR
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor.Companion.CAPTURE_BITMAP_SUCCESS
import com.codingwithmitch.giffit.interactors.PixelCopyJob
import com.codingwithmitch.giffit.interactors.PixelCopyJobInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import java.util.UUID

class MainViewModel : ViewModel() {

    private val dispatcher = IO
    private val pixelCopy: PixelCopyJob = PixelCopyJobInteractor()
    private val captureBitmaps: CaptureBitmaps = CaptureBitmapsInteractor(
        pixelCopyJob = pixelCopy
    )

    private val _state: MutableState<MainState> = mutableStateOf(Initial)
    val state: State<MainState> get() = _state
    private val _toastEventRelay: MutableStateFlow<ToastEvent?> = MutableStateFlow(null)
    val toastEventRelay: StateFlow<ToastEvent?> get() = _toastEventRelay
    private val _errorRelay: MutableStateFlow<Set<ErrorEvent>> = MutableStateFlow(setOf())
    val errorRelay: StateFlow<Set<ErrorEvent>> get() = _errorRelay

    fun runBitmapCaptureJob(
        view: View,
        window: Window
    ) {
        val state = state.value
        check(state is DisplayBackgroundAsset) { "Invalid state: ${state}" }
        // We need a way to stop the job if a user presses "STOP". So create a Job for this.
        val bitmapCaptureJob = Job()
        // Create convenience function for checking if the user pressed "STOP".
        val checkShouldCancelJob: () -> Unit = {  ->
            // TODO("Add logic for determining if state is correct before canceling")
//            bitmapCaptureJob.cancel(CAPTURE_BITMAP_SUCCESS)
        }
        // Execute the use-case.
        captureBitmaps.execute(
            capturingViewBounds = state.capturingViewBounds,
            window = window,
            view = view,
        ).onEach { dataState ->
            // If the user hits the "STOP" button, complete the job by canceling.
            checkShouldCancelJob()
            when(dataState) {
                is DataState.Data -> {
                    dataState.data?.let { bitmaps ->
                        _state.value = state.copy(
                            capturedBitmaps = bitmaps
                        )
                    }
                }
                is DataState.Error -> {
                    // For this use-case, if an error occurs we need to stop the job.
                    // Otherwise it will keep trying to capture bitmaps and failing over and over.
                    bitmapCaptureJob.cancel(CAPTURE_BITMAP_ERROR)
                    // TODO("Update loading state")
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = dataState.message
                        )
                    )
                }
                is DataState.Loading -> {
                    // TODO("Update loading state")
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope + bitmapCaptureJob).invokeOnCompletion { throwable ->
            // TODO("Update loading state")
            val onSuccess: () -> Unit = {
                // TODO("Build the gif from the list of captured bitmaps")
                val newState = _state.value
                if (newState is DisplayBackgroundAsset) {
                    Log.d("TAG", "runBitmapCaptureJob: Num bitmaps: ${newState.capturedBitmaps.size}")
                }
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
}