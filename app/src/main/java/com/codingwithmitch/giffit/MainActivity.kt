package com.codingwithmitch.giffit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
import kotlin.math.*

val TAG = "MitchsLog"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val configuration = LocalConfiguration.current
                    ConstraintLayout(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val (topBar, assetBox, bottomContainer) = createRefs()
                        // Top bar
                        // topBarHeight = (default app bar height) + (button padding)
                        val saveBtnPadding = 16.dp
                        val topBarHeight = remember { 56.dp + saveBtnPadding }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topBarHeight)
                                .constrainAs(topBar) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                                .zIndex(2f)
                                .background(Color.White)
                        ) {
                            Button(
                                modifier = Modifier
                                    .padding(saveBtnPadding)
                                    .align(Alignment.End),
                                onClick = {
                                    // TODO("Save image")
                                }
                            ) {
                                Text("Save")
                            }
                        }

                        // Background with asset
                        var activeShapeId by remember { mutableStateOf("one") }
                        var isActiveShapeInMotion by remember { mutableStateOf(false) }
                        val backgroundHeight = remember { (configuration.screenHeightDp * 0.6).toInt() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray)
                                .height(backgroundHeight.dp)
                                .zIndex(1f)
                                .constrainAs(assetBox) {
                                    top.linkTo(topBar.bottom)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                        ) {
                            samples.forEach {
                                RenderShape(
                                    shapeData = it,
                                    isEnabled = it.id == activeShapeId,
                                    onGestureStarted = {
                                        if (!isActiveShapeInMotion) {
                                            activeShapeId = it.id
                                        }
                                        isActiveShapeInMotion = true
                                    },
                                    onGestureFinished = {
                                        isActiveShapeInMotion = false
                                    },
                                    backgroundHeightDp = backgroundHeight,
                                    screenWidthDp = configuration.screenWidthDp
                                )
                            }
                        }
                        // Bottom container with the remaining view height
                        val remainingHeight = remember { configuration.screenHeightDp.dp - topBarHeight - backgroundHeight.dp }
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxWidth()
                                .height(remainingHeight)
                                .zIndex(2f)
                                .constrainAs(bottomContainer) {
                                    bottom.linkTo(parent.bottom)
                                    top.linkTo(assetBox.bottom)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                        )
                    }
                }
            }
        }
    }
}

private val samples: MutableList<ShapeData> = mutableListOf(
    ShapeData(
        id = "one",
        color = Color.Blue,
        initialOffset = Offset.Zero,
        initialSize = Size(200f, 200f)
    ),
    ShapeData(
        id = "two",
        color = Color.Black,
        initialOffset = Offset(-200f, -200f),
        initialSize = Size(200f, 200f)
    )
)

data class ShapeData(
    val id: String,
    val color: Color,
    val initialOffset: Offset,
    val initialSize: Size,
)

@Composable
fun RenderShape(
    shapeData: ShapeData,
    isEnabled: Boolean,
    onGestureStarted: () -> Unit,
    onGestureFinished: () -> Unit,
    screenWidthDp: Int,
    backgroundHeightDp: Int,
) {
    var offset by remember { mutableStateOf(shapeData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(shapeData.initialSize) }

    val xOffset = with(LocalDensity.current) { -offset.x.toDp() }
    val yOffset = with(LocalDensity.current) { -offset.y.toDp() }

    // Not sure why I couldn't just use isEnabled but it doesn't work somehow.
    var enabled by remember { mutableStateOf(isEnabled) }
    if (enabled != isEnabled) {
        enabled = isEnabled
    }

    Box(
        modifier = Modifier
            .zIndex(
                if (enabled) {
                    2f
                } else {
                    1f
                }
            )
            .size(width = size.width.dp, height = size.height.dp)
            .offset(
                x = xOffset,
                y = yOffset
            )
            .pointerInput(Unit) {
                detectTransformGesturesAndTouch(
                    onGestureStarted = {
                        Log.d(TAG, "Gesture STARTED: ${shapeData.id}")
                        onGestureStarted()
                    },
                    onGestureFinished = {
                        Log.d(TAG, "Gesture FINISHED: ${shapeData.id}")
                        onGestureFinished()
                    },
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        if (!enabled) return@detectTransformGesturesAndTouch

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
                            // For natural zooming and rotating, the centroid of the gesture should
                            // be the fixed point where zooming and rotating occurs.
                            // We compute where the centroid was (in the pre-transformed coordinate
                            // space), and then compute where it will be after this delta.
                            // We then compute what the new offset should be to keep the centroid
                            // visually stationary for rotating and zooming, and also apply the pan.

                            (offset + centroid / oldScale).rotateBy(gestureRotate) -
                                    (centroid / newScale + pan / oldScale)
                        }
                        offset = newOffset

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
                        val bottomYBound = (-backgroundHeightDp.dp + b)
                        if (offset.y.toDp() < bottomYBound) {
                            offset = Offset(
                                x = offset.x,
                                y = bottomYBound.toPx()
                            )
                        }

                        size = newSize
                        zoom = newScale
                        angle += gestureRotate
//                        Log.d(TAG, "offsetX: ${offset.x}")
//                        Log.d(TAG, "offsetY: ${offset.y}")
//                        Log.d(TAG, "centroid: ${centroid}")
//                        Log.d(TAG, "pan: ${centroid}")
//                        Log.d(TAG, "gestureZoom: ${gestureZoom}")
//                        Log.d(TAG, "currentZoom: ${zoom}")
//                        Log.d(TAG, "gestureRotate: ${gestureRotate}")
                    }
                )
            }
            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin.Center
            }
            .clip(RectangleShape)
            .background(shapeData.color),
    )
}

suspend fun PointerInputScope.detectTransformGesturesAndTouch(
    panZoomLock: Boolean = false,
    onGestureStarted: () -> Unit,
    onGestureFinished: () -> Unit,
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

            val firstDown = awaitFirstDown(requireUnconsumed = false)
            if (firstDown.changedToDown()) {
                onGestureStarted()
            }
            do {
                val event = awaitPointerEvent()

                val finished = event.changes.fastAny { it.changedToUp() }
                if (finished) {
                    onGestureFinished()
                }

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

@Composable
fun MySample() {
    Box(modifier = Modifier.fillMaxSize()) {
        val image: Painter = painterResource(id = R.drawable.mitch)
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = image,
            contentDescription = "",
            contentScale = ContentScale.FillWidth,
        )

        var offset by remember { mutableStateOf(Offset.Zero) }
        var scale by remember { mutableStateOf(1f) }
        var rotation by remember { mutableStateOf(0f) }
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            scale *= zoomChange
            rotation += rotationChange
            offset += offsetChange
        }
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(
                    state = state
                )
            ,
        ) {
            val selectedResource: Painter = painterResource(id = R.drawable.deal_with_it_sunglasses_default)
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = selectedResource,
                contentDescription = ""
            )
        }
    }
}

@Composable
fun TransformableSample() {
    // set up all transformation states
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
    }
    Box(
        Modifier
            // apply other transformations like rotation and zoom
            // on the pizza slice emoji
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            // add transformable to listen to multitouch transformation events
            // after offset
            .transformable(
                state = state,
            )
            .background(Color.Blue)
            .fillMaxSize()
    )
}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

