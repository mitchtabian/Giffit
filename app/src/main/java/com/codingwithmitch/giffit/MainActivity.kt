package com.codingwithmitch.giffit

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getContentUri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


val TAG = "MitchsLog"

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileToScopedStorage(displayName: String, bitmap: Bitmap) {
        val externalUri: Uri = getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // Add content values so media is discoverable by android and added to common directories.
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
        contentResolver.insert(externalUri, contentValues)?.let { fileUri ->
            try {
                val outputStream: OutputStream? = contentResolver.openOutputStream(fileUri)
                outputStream.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                    out?.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveFileToStorage(displayName: String, bmp: Bitmap) {
        val file = File("${Environment.getExternalStorageDirectory()}/Pictures", "$displayName.png")
        if (!file.exists()) {
            try {
                // Add content values so media is discoverable by android and added to common directories.
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.let { fos ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 85, fos)
                        fos.flush()
                        fos.close()
                        Log.d(TAG, "saveFileToStorage: ${file}")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermissions(): Boolean  {
        val writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    private val externalStoragePermissionRequest = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                Toast.makeText(this@MainActivity, "To enable this permission you'll have to do so in system settings for this app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Scoped storage doesn't exist before Android 29 so need to check permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!checkPermissions()) {
                externalStoragePermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    )
                )
            }
        }
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val view = LocalView.current
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .align(Alignment.End)
                            ,
                            onClick = {
                                // If API >= 29 we can use scoped storage and don't require permission to save images.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val bmp = Bitmap.createBitmap(
                                        view.width, view.height,
                                        Bitmap.Config.ARGB_8888
                                    ).applyCanvas {
                                        view.draw(this)
                                    }
                                    saveFileToScopedStorage("screenshot", bmp)
                                } else {
                                    // Scoped storage doesn't exist before Android 29 so need to check permissions
                                    if (checkPermissions()) {
                                        val bmp = Bitmap.createBitmap(
                                            view.width, view.height,
                                            Bitmap.Config.ARGB_8888
                                        ).applyCanvas {
                                            view.draw(this)
                                        }
                                        saveFileToStorage("screenshot", bmp)
                                    } else {
                                        externalStoragePermissionRequest.launch(
                                            arrayOf(
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                        var activeShapeId by remember { mutableStateOf("one") }
                        var isActiveShapeInMotion by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray)
                        ) {
                            val image: Painter = painterResource(id = R.drawable.mitch)
                            Image(
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                                painter = image,
                                contentDescription = ""
                            )
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
                                    }
                                )
                            }
                        }
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
) {
    var offset by remember { mutableStateOf(shapeData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(shapeData.initialSize) }

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
            .offset {
                IntOffset(
                    -offset.x.toInt(),
                    -offset.y.toInt()
                )
            }
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
                ),
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

