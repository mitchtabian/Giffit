package com.codingwithmitch.giffit

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

sealed class MainState {

    object Initial: MainState()

    data class DisplaySelectBackgroundAsset(
        val backgroundAssetPickerLauncher: ActivityResultLauncher<String>
    ): MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
        val backgroundAssetPickerLauncher: ActivityResultLauncher<String>
    ): MainState()
}
