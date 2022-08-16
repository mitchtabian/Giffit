package com.codingwithmitch.giffit.ui

import android.net.Uri
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.codingwithmitch.giffit.domain.util.AssetData
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.ui.compose.RecordActionBar
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
    updateCapturingViewBounds: (Rect) -> Unit,
    bitmapCapture: MainLoadingState,
    launchImagePicker: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (topBar, assetContainer, bottomContainer) = createRefs()

        // Top bar
        // topBarHeight = (default app bar height) + (button padding)
        val topBarHeight = remember { 56 + 16 }
        RecordActionBar(
            modifier = Modifier
                .height(topBarHeight.dp)
                .background(Color.White)
                .constrainAs(topBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .zIndex(2f)
            ,
            bitmapCaptureLoadingState = bitmapCaptureLoadingState,
            updateBitmapCaptureJobState = updateBitmapCaptureJobState,
            startBitmapCaptureJob = startBitmapCaptureJob
        )

        // Gif capture area
        val configuration = LocalConfiguration.current
        val assetContainerHeight = remember { (configuration.screenHeightDp * 0.6).toInt() }
        RenderBackground(
            modifier = Modifier
                .constrainAs(assetContainer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topBar.bottom)
                }
                .zIndex(1f)
            ,
            backgroundAsset = backgroundAssetUri,
            assetData = assetData,
            updateCapturingViewBounds = updateCapturingViewBounds,
            screenWidthDp = configuration.screenWidthDp,
            assetContainerHeightDp = assetContainerHeight
        )

        // Bottom container
        val bottomContainerHeight = remember { configuration.screenHeightDp - assetContainerHeight - topBarHeight }
        BackgroundAssetFooter(
            modifier = Modifier
                .background(Color.White)
                .height(bottomContainerHeight.dp)
                .constrainAs(bottomContainer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(assetContainer.bottom)
                    bottom.linkTo(parent.bottom)
                }
                .zIndex(2f)
            ,
            bitmapCapture = bitmapCapture
        ) {
            launchImagePicker()
        }
    }
}

@Composable
fun RenderBackground(
    modifier: Modifier,
    backgroundAsset: Uri?,
    assetData: AssetData,
    updateCapturingViewBounds: (Rect) -> Unit,
    screenWidthDp: Int,
    assetContainerHeightDp: Int,
) {
    Box(
        modifier = modifier
            .wrapContentSize()
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(assetContainerHeightDp.dp)
                .onGloballyPositioned {
                    updateCapturingViewBounds(it.boundsInRoot())
                }
            ,
            model = backgroundAsset,
            contentScale = ContentScale.Crop,
            contentDescription = ""
        )
        RenderAsset(
            assetData = assetData,
            screenWidthDp = screenWidthDp,
            assetContainerHeightDp = assetContainerHeightDp
        )
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
    screenWidthDp: Int,
    assetContainerHeightDp: Int,
) {
    var offset by remember { mutableStateOf(assetData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(assetData.initialSize) }

    val xOffset = with(LocalDensity.current) { -offset.x.toDp() }
    val yOffset = with(LocalDensity.current) { -offset.y.toDp() }

    Box (
        modifier = Modifier
            .width(screenWidthDp.dp)
            .height(assetContainerHeightDp.dp)
    ) {
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
                            var newSize = size * gestureZoom

                            // Restrict the height
                            if (newSize.height >= assetContainerHeightDp) {
                                newSize = Size(
                                    width = newSize.width,
                                    height = assetContainerHeightDp.toFloat()
                                )
                            }

                            // Restrict the width
                            if (newSize.width >= screenWidthDp) {
                                newSize = Size(
                                    width = screenWidthDp.toFloat(),
                                    height = newSize.height
                                )
                            }

                            // If we're only moving the shape we need to put bounds on how fast it can move.
                            // Otherwise as it gets smaller it will move faster. And as it gets bigger it will
                            // move slower.
                            // If zoom has not changed, limit movement speed.
                            val newOffset = if (newSize == size) {
                                offset - pan
                            } else { // We're zooming/rotating
                                // https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTransformGestures(kotlin.Boolean,kotlin.Function4)
                                (offset + centroid / oldScale).rotateBy(gestureRotate) -
                                        (centroid / newScale + pan / oldScale)
                            }
                            offset = newOffset

                            // Restrict the offset
                            val a = size.width.dp / 2
                            val b = size.height.dp / 2

                            // Start bound
                            if (offset.x.toDp() >= a) {
                                offset = Offset(
                                    x = a.toPx(),
                                    y = offset.y
                                )
                            }

                            // Top bound
                            if (offset.y.toDp() >= b) {
                                offset = Offset(
                                    x = offset.x,
                                    y = b.toPx()
                                )
                            }

                            // End-x bound
                            val endXBound = (-screenWidthDp.dp + a)
                            if (offset.x.toDp() < endXBound) {
                                offset = Offset(
                                    x = endXBound.toPx(),
                                    y = offset.y
                                )
                            }

                            // Bottom-y bound
                            val bottomYBound = (-assetContainerHeightDp.dp + b)
                            if (offset.y.toDp() < bottomYBound) {
                                offset = Offset(
                                    x = offset.x,
                                    y = bottomYBound.toPx()
                                )
                            }

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
                .zIndex(1f)
            ,
            painter = painter,
            contentDescription = ""
        )
    }
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