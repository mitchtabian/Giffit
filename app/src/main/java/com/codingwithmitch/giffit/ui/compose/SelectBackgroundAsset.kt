package com.codingwithmitch.giffit.ui.compose

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SelectBackgroundAsset(
    backgroundAssetPickerLauncher: ActivityResultLauncher<String>
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Button(
            modifier = Modifier
                .align(Alignment.Center),
            onClick = {
                backgroundAssetPickerLauncher.launch("image/*")
            }
        ) {
            Text("Choose background image")
        }
    }
}