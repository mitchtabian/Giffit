package com.codingwithmitch.giffit.ui

import android.net.Uri
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.codingwithmitch.giffit.domain.util.AssetData
import com.codingwithmitch.giffit.domain.DataState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BackgroundAsset(
    bitmapCaptureLoadingState: MainLoadingState.BitmapCapture?,
    updateBitmapCaptureJobState: (DataState.Loading.LoadingState) -> Unit,
    startBitmapCaptureJob: (View) -> Unit,
    backgroundAssetUri: Uri,
    assetData: AssetData,
    updateCapturingViewBounds: (Rect) -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
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
    RenderBackground(
        backgroundAsset = backgroundAssetUri,
        assetData = assetData,
        updateCapturingViewBounds = updateCapturingViewBounds
    )
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
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height((configuration.screenHeightDp * 0.6).dp)
                .onGloballyPositioned {
                    updateCapturingViewBounds(it.boundsInRoot())
                }
            ,
            model = backgroundAsset,
            contentScale = ContentScale.Crop,
            contentDescription = ""
        )
        RenderAsset(assetData = assetData)
    }
}

@Composable
fun RecordButton(
    modifier: Modifier,
    isRecording: Boolean,
    updateBitmapCaptureJobState: (DataState.Loading.LoadingState) -> Unit,
    startBitmapCaptureJob: () -> Unit,
) {
    Button(
        modifier = modifier
            .wrapContentWidth()
            .zIndex(3f) // TODO("asset can be above the button somehow, need to fix")
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
                true -> DataState.Loading.LoadingState.Idle
                false -> DataState.Loading.LoadingState.Active()
            }
            updateBitmapCaptureJobState(newJobState)
            if (newJobState is DataState.Loading.LoadingState.Active) { // Start recording
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
private fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}