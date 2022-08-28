package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.geometry.Rect

sealed class MainState {

    object Initial: MainState()

    data class DisplaySelectBackgroundAsset(
        val backgroundAssetPickerLauncher: ActivityResultLauncher<String>
    ): MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val capturingViewBounds: Rect? = null,
        val capturedBitmap: Bitmap?
    ): MainState()
}
