package com.codingwithmitch.giffit

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class AssetData(
    @DrawableRes val id: Int,
    val initialOffset: Offset,
    val initialSize: Size,
)