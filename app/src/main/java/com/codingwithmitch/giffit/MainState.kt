package com.codingwithmitch.giffit

import android.net.Uri

sealed class MainState {

    object Initial: MainState()

    object DisplaySelectBackgroundAsset: MainState()

    data class DisplayBackgroundAsset(
        val backgroundAssetUri: Uri,
    ): MainState()
}
