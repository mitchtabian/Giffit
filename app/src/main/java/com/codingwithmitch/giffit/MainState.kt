package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.geometry.Rect
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*

sealed class MainState {

    object Initial: MainState()

    data class DisplaySelectBackgroundAsset(
        val backgroundAssetPickerLauncher: ActivityResultLauncher<String>
    ): MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmaps: List<Bitmap> = listOf(),

        // Displayed as a LinearProgressIndicator in the RecordActionBar
        val bitmapCaptureLoadingState: LoadingState = Idle
    ): MainState()
}
