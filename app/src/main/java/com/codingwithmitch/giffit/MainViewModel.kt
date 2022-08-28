package com.codingwithmitch.giffit

import android.annotation.SuppressLint
import android.view.View
import android.view.Window
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.interactors.CaptureBitmaps
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor
import com.codingwithmitch.giffit.interactors.PixelCopyJob
import com.codingwithmitch.giffit.interactors.PixelCopyJobInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    // Suppress warning for now. We're just testing this.
    @SuppressLint("NewApi")
    fun captureScreenshot(
        view: View,
        window: Window
    ) {
        val state = state.value
        check(state is DisplayBackgroundAsset) { "Invalid state: ${state}" }
        CoroutineScope(dispatcher).launch {
            val result = pixelCopy.execute(
                capturingViewBounds = state.capturingViewBounds,
                view = view,
                window = window
            )
            when (result) {
                is PixelCopyJob.PixelCopyJobState.Done -> {
                    _state.value = state.copy(capturedBitmap = result.bitmap)
                }
                is PixelCopyJob.PixelCopyJobState.Error -> {
                    publishErrorEvent(
                        ErrorEvent(
                            id = UUID.randomUUID().toString(),
                            message = result.message
                        )
                    )
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