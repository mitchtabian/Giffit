package com.codingwithmitch.giffit.ui.compose

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.*

@Composable
fun Gif(
    imageLoader: ImageLoader,
    gifUri: Uri?,
    discardGif: () -> Unit,
    onSavedGif: () -> Unit,
    resetGifToOriginal: () -> Unit,
    isResizedGif: Boolean,
    currentGifSize: Int,
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    resizeGif: () -> Unit,
    gifResizingLoadingState: LoadingState,
    loadingState: LoadingState,
) {
    StandardLoadingUI(loadingState = loadingState)
    ResizingGifLoadingUI(gifResizingLoadingState = gifResizingLoadingState)
    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (gifUri != null) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = discardGif,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red
                        )
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Discard",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSavedGif,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Green
                        ),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
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
                GifFooter(
                    adjustedBytes = adjustedBytes,
                    updateAdjustedBytes = updateAdjustedBytes,
                    sizePercentage = sizePercentage,
                    updateSizePercentage = updateSizePercentage,
                    gifSize = currentGifSize,
                    isResizedGif = isResizedGif,
                    resetResizing = resetGifToOriginal,
                    resizeGif = resizeGif
                )
            }
        }
    }
}

@Composable
fun GifFooter(
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    gifSize: Int,
    isResizedGif: Boolean,
    resizeGif: () -> Unit,
    resetResizing: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
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
        if (isResizedGif) {
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = resetResizing
            ) {
                Text(
                    text = "Reset resizing",
                    style = MaterialTheme.typography.body1,
                )
            }
        } else {
            Text(
                text = "$sizePercentage %",
                style = MaterialTheme.typography.body1,
            )
            var sliderPosition by remember { mutableStateOf(100f) }
            Slider(
                value = sliderPosition,
                valueRange = 1f..100f,
                onValueChange = {
                    sliderPosition = it
                    updateSizePercentage(sliderPosition.toInt())
                    updateAdjustedBytes(gifSize * sliderPosition.toInt() / 100)
                },
            )
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = resizeGif
            ) {
                Text(
                    text = "Resize",
                    style = MaterialTheme.typography.body1,
                )
            }
        }
    }
}
