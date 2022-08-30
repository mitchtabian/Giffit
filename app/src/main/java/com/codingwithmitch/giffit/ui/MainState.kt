package com.codingwithmitch.giffit.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.codingwithmitch.giffit.domain.util.AssetData
import com.codingwithmitch.giffit.R
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*

sealed class MainState {

    data class Initial(
        // Displayed as a CircularIndeterminateProgressBar overlayed in the center of the screen.
        val loadingState: LoadingState = Idle
    ): MainState()

    data class DisplaySelectBackgroundAsset(
        val backgroundAssetPickerLauncher: ActivityResultLauncher<String>,
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

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),

        // Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: LoadingState = Idle,

        // Displayed as a CircularIndeterminateProgressBar overlayed in the center of the screen.
        val loadingState: LoadingState = Idle,
    ): MainState() {
        val assetData: AssetData = AssetData(
            id = R.drawable.deal_with_it_sunglasses_default,
            initialOffset = Offset(0f, 0f),
            initialSize = Size(200f, 200f)
        )
    }
}