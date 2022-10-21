package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*

sealed class MainState {

    object Initial: MainState()

    object DisplaySelectBackgroundAsset: MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),

        // Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: LoadingState = Idle,

        // Displayed as a CircularIndeterminateProgressBar overlayed in the center of the screen.
        val loadingState: LoadingState = Idle,
    ): MainState()

    data class DisplayGif(
        val gifUri: Uri?,
        val resizedGifUri: Uri?,
        val originalGifSize: Int,
        val adjustedBytes: Int,
        val sizePercentage: Int,
        val backgroundAssetUri: Uri,
        val capturedBitmaps: List<Bitmap> = listOf(),

        // Displayed as a LinearProgressIndicator in the middle of the screen, occupying the entire view.
        val resizeGifLoadingState: LoadingState = Idle,

        // Displayed as a CircularIndeterminateProgressBar overlayed in the center of the screen.
        val loadingState: LoadingState = Idle,
    ): MainState()
}
