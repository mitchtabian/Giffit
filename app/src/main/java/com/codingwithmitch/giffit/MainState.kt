package com.codingwithmitch.giffit

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

sealed class MainState {

    object Initial: MainState()

    object DisplaySelectBackgroundAsset: MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
    ): MainState()
}
