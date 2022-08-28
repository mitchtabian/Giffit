package com.codingwithmitch.giffit.ui.compose

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
    launchImagePicker: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Button(
            modifier = Modifier
                .align(Alignment.Center),
            onClick = {
                launchImagePicker()
            }
        ) {
            Text("Choose background image")
        }
    }
}