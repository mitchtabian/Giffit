package com.codingwithmitch.giffit.ui.compose

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.ui.MainLoadingState.*
import com.codingwithmitch.giffit.ui.RecordButton

@Composable
fun RecordActionBar(
    modifier: Modifier,
    bitmapCaptureLoadingState: BitmapCapture?,
    updateBitmapCaptureJobState: (DataState.Loading.LoadingState) -> Unit,
    startBitmapCaptureJob: (View) -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(50.dp)
                .background(Color.Transparent)
        ) {
            when(val loadingState = bitmapCaptureLoadingState?.loadingState) {
                is DataState.Loading.LoadingState.Active -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(4.dp))
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                        ,
                        progress = loadingState.progress ?: 0f,
                        backgroundColor = Color.White,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
        val isRecording = bitmapCaptureLoadingState != null &&
                bitmapCaptureLoadingState.loadingState is DataState.Loading.LoadingState.Active &&
                (bitmapCaptureLoadingState.loadingState.progress ?: 0f) > 0f
        RecordButton(
            modifier = Modifier.weight(1f),
            isRecording = isRecording,
            updateBitmapCaptureJobState = updateBitmapCaptureJobState,
            startBitmapCaptureJob = {
                startBitmapCaptureJob(view)
            },
        )
    }
}