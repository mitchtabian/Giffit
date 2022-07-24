package com.codingwithmitch.giffit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.BitmapCaptureJobState
import com.codingwithmitch.giffit.MainViewModel.*

@Composable
fun Footer(
    mainState: MainState,
    launchImagePicker: () -> Unit,
) {
    val pair = if (mainState is MainState.DisplayBackgroundAsset) {
        Pair(
            mainState.backgroundAssetUri,
            mainState.bitmapCaptureJobState == BitmapCaptureJobState.Running
        )
    } else {
        null
    }
    // If there is a background asset AND the bitmap capture job is not running, show footer.
    if (pair?.first != null && !pair.second) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 16.dp),
                onClick = {
                    launchImagePicker()
                }
            ) {
                Text("Change background image")
            }
        }
    }
}