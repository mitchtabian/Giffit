package com.codingwithmitch.giffit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordActionBar(
    modifier: Modifier,
    isRecording: Boolean,
    updateIsRecording: (Boolean) -> Unit,
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
            // TODO("Add progress bar for when gif is recording")
        }
        RecordButton(
            modifier = Modifier.weight(1f),
            isRecording = isRecording,
            updateIsRecording = updateIsRecording
        )
    }
}

@Composable
fun RecordButton(
    modifier: Modifier,
    isRecording: Boolean,
    updateIsRecording: (Boolean) -> Unit,
) {

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
            updateIsRecording(!isRecording)
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