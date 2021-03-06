package com.codingwithmitch.giffit

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.codingwithmitch.giffit.BitmapCaptureJobState.*
import com.codingwithmitch.giffit.domain.Constants.TAG
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RecordButton(
    isRecording: Boolean,
    updateBitmapCaptureJobState: (BitmapCaptureJobState) -> Unit,
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
                val newJobState = when(isRecording) {
                    true -> Idle
                    false -> Running
                }
                updateBitmapCaptureJobState(newJobState)
                if (newJobState == Running) { // Start recording
                    startBitmapCaptureJob()
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
    launchImagePicker: () -> Unit,
) {
    if (backgroundAsset != null && !isRecording) {
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
    resetGifToOriginal: () -> Unit,
    isResizedGif: Boolean,
    onSavedGif: () -> Unit,
    currentGifSize: Int,
    adjustedBytes: Int,
    updateAdjustedBytes: (Int) -> Unit,
    sizePercentage: Int,
    updateSizePercentage: (Int) -> Unit,
    resizeGif: () -> Unit,
) {
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
                        onClick = {
                            discardGif()
                        },
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
                        onClick = {
                            onSavedGif()
                        },
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
        if (isBuildingGif) {
            Log.d(TAG, "isBuildingGif: ${isBuildingGif}")
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
                valueRange = 5f..100f,
                onValueChange = {
                    sliderPosition = it
                    val newSizePercentage = sliderPosition.toInt()
                    updateSizePercentage(newSizePercentage)
                    updateAdjustedBytes(gifSize * newSizePercentage / 100)
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

/**
 * @param progress: value between 0..100 representing the progress of the resize job.
 */
@Composable
fun ResizingGifProgressBar(
    progress: Float,
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start).padding(vertical = 12.dp),
                text = "Resizing gif...",
                style = MaterialTheme.typography.h5,
                color = Color.White
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                ,
                progress = progress,
                backgroundColor = Color.White,
                color = MaterialTheme.colors.primary
            )
        }

    }
}

@Composable
fun RenderAsset(
    assetData: AssetData,
) {
    var offset by remember { mutableStateOf(assetData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(assetData.initialSize) }

    val xOffset = with(LocalDensity.current) { -offset.x.toDp() }
    val yOffset = with(LocalDensity.current) { -offset.y.toDp() }

    val painter: Painter = rememberAsyncImagePainter(model = assetData.id)
    Image(
        modifier = Modifier
            .size(width = size.width.dp, height = size.height.dp)
            .offset(
                x = xOffset,
                y = yOffset
            )
            .pointerInput(Unit) {
                detectTransformGesturesAndTouch(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        val oldScale = zoom
                        val newScale = zoom * gestureZoom

                        // If we're only moving the shape we need to put bounds on how fast it can move.
                        // Otherwise as it gets smaller it will move faster. And as it gets bigger it will
                        // move slower.
                        val newSize = size * gestureZoom
                        // If zoom has not changed, limit movement speed.
                        val newOffset = if (newSize == size) {
                            offset - pan
                        } else { // We're zooming/rotating
                            // https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTransformGestures(kotlin.Boolean,kotlin.Function4)
                            (offset + centroid / oldScale).rotateBy(gestureRotate) -
                                    (centroid / newScale + pan / oldScale)
                        }
                        offset = newOffset
                        size = newSize
                        zoom = newScale
                        angle += gestureRotate
                    }
                )
            }
            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin.Center
            }
            .clip(RectangleShape)
        ,
        painter = painter,
        contentDescription = ""
    )
}

private suspend fun PointerInputScope.detectTransformGesturesAndTouch(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            awaitFirstDown(requireUnconsumed = false)

            do {
                val event = awaitPointerEvent()

                val canceled = event.changes.fastAny { it.positionChangeConsumed() }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            rotationMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            pastTouchSlop = true
                            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                        }
                    }

                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                        if (effectiveRotation != 0f ||
                            zoomChange != 1f ||
                            panChange != Offset.Zero
                        ) {
                            onGesture(centroid, panChange, zoomChange, effectiveRotation)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consumeAllChanges()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
        }
    }
}

/**
 * https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTransformGestures(kotlin.Boolean,kotlin.Function4)
 */
fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}







