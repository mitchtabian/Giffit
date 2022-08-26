package com.codingwithmitch.giffit.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.ui.MainLoadingState

@Composable
fun BackgroundAssetFooter(
    modifier: Modifier,
    bitmapCapture: MainLoadingState,
    launchImagePicker: () -> Unit,
) {
    // If bitmap capture job is not running, show footer.
    if (bitmapCapture.loadingState !is DataState.Loading.LoadingState.Active) {
        Column(
            modifier = modifier
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