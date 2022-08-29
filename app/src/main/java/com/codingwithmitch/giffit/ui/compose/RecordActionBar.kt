package com.codingwithmitch.giffit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*

@Composable
fun RecordActionBar(
    modifier: Modifier,
    bitmapCaptureLoadingState: LoadingState,
    startBitmapCaptureJob: () -> Unit,
    endBitmapCaptureJob: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(50.dp)
                .background(Color.Transparent)
        ) {
            when(bitmapCaptureLoadingState) {
                is Active -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(4.dp))
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                        ,
                        progress = bitmapCaptureLoadingState.progress ?: 0f,
                        backgroundColor = Color.White,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
        RecordButton(
            modifier = Modifier.weight(1f),
            bitmapCaptureLoadingState = bitmapCaptureLoadingState,
            startBitmapCaptureJob = startBitmapCaptureJob,
            endBitmapCaptureJob = endBitmapCaptureJob
        )
    }
}

@Composable
fun RecordButton(
    modifier: Modifier,
    bitmapCaptureLoadingState: LoadingState,
    startBitmapCaptureJob: () -> Unit,
    endBitmapCaptureJob: () -> Unit,
) {
    val isRecording = when(bitmapCaptureLoadingState) {
        is Active -> true
        Idle -> false
    }
    Button(
        modifier = modifier
            .wrapContentWidth()
        ,
        colors = if (isRecording) {
            ButtonDefaults.buttonColors(
                backgroundColor = Color.Red
            )
        } else {
            ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        onClick = {
            if (!isRecording) {
                startBitmapCaptureJob()
            } else {
                endBitmapCaptureJob()
            }
        }
    ) {
        Text(
            text = if (isRecording) {
                "End"
            } else {
                "Record"
            }
        )
    }
}