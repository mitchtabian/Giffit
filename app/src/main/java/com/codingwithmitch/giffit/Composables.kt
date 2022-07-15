package com.codingwithmitch.giffit

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter

@Composable
fun RecordButton(
    isRecording: Boolean,
    updateBitmapCaptureJob: (Boolean) -> Unit,
    startBitmapCaptureJob: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .zIndex(3f)
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
                val shouldRecord = !isRecording
                updateBitmapCaptureJob(shouldRecord)
                if (shouldRecord) { // Start recording
                    startBitmapCaptureJob()
                } else { // End recording
                    updateBitmapCaptureJob(false)
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
}

@Composable
fun Footer(
    backgroundAsset: Uri?,
    isRecording: Boolean,
    croppedImageSize: Int,
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    launchImagePicker: () -> Unit,
) {
    if (backgroundAsset != null && !isRecording) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (croppedImageSize > 0) {
                LaunchedEffect(key1 = croppedImageSize) {
                    updateAdjustedBytes(croppedImageSize)
                }
                Text(
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.h6,
                    text = "Approximate gif size"
                )
                Text(
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.body1,
                    text = "${adjustedBytes / 1024} KB" // convert to bytes -> KB
                )
                Text(text = "$sizePercentage")
                var sliderPosition by remember { mutableStateOf(100f) }
                Slider(
                    value = sliderPosition,
                    valueRange = 5f..100f,
                    onValueChange = {
                        sliderPosition = it
                        val newSizePercentage = sliderPosition.toInt()
                        updateSizePercentage(newSizePercentage)
                        updateAdjustedBytes(croppedImageSize * newSizePercentage / 100)
                    },
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(25.dp)
                        .align(Alignment.End),
                    color = Color.Blue,
                    strokeWidth = 2.dp
                )
            }
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

@Composable
fun RenderBackground(
    backgroundAsset: Uri?,
    assetData: AssetData,
    updateCapturingViewBounds: (Rect) -> Unit,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
    ) {
        val configuration = LocalConfiguration.current
        val image: Painter = rememberAsyncImagePainter(model = backgroundAsset)
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height((configuration.screenHeightDp * 0.6).dp)
                .onGloballyPositioned {
                    updateCapturingViewBounds(it.boundsInRoot())
                }
            ,
            contentScale = ContentScale.Crop,
            painter = image,
            contentDescription = ""
        )
        RenderAsset(assetData = assetData)
    }
}

@Composable
fun DisplayGif(
    gifUri: Uri?,
    imageLoader: ImageLoader,
    isBuildingGif: Boolean,
    discardGif: () -> Unit,
    onSavedGif: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (gifUri != null) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.Center)
            ) {
                val image: Painter = rememberAsyncImagePainter(model = gifUri, imageLoader = imageLoader)
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((configuration.screenHeightDp * 0.6).dp)
                    ,
                    contentScale = ContentScale.Crop,
                    painter = image,
                    contentDescription = ""
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            onSavedGif()
                        }
                    ) {
                        Text("Keep")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            discardGif()
                        }
                    ) {
                        Text("Discard")
                    }
                }
            }
        }
        if (isBuildingGif) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
                color = Color.Blue,
                strokeWidth = 4.dp
            )
        }
    }
}







