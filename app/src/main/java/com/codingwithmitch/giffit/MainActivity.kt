package com.codingwithmitch.giffit

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getContentUri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.math.*


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
                        var capturingViewBounds by remember { mutableStateOf<Rect?>(null) }
                        var isRecording by remember { mutableStateOf(false) }
                        var totalCaptureTime by remember { mutableStateOf(0) }
                        var previousCaptureTime by remember { mutableStateOf(System.currentTimeMillis()) }

                        val resetRecording: () -> Unit = {
                            Log.d(TAG, "Total captured bitmaps: ${capturedBitmaps.size}")
                            capturedBitmaps.forEach { bmp ->
                                Log.d(TAG, "Captured Bitmap: ${bmp}")
                            }
                            totalCaptureTime = 0
                            capturedBitmaps.clear()
                        }
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .align(Alignment.End)
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
                                isRecording = !isRecording
                                if (isRecording) { // Start recording
                                    previousCaptureTime = System.currentTimeMillis()
                                } else { // End recording
                                    resetRecording()
                                }
                            }
                        ) {
                            if (isRecording) {
                                Text("End")
                            } else {
                                Text("Start")
                            }
                        }
                        var activeShapeId by remember { mutableStateOf(-1) }
                        var isActiveShapeInMotion by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                        ) {
                            val image: Painter = painterResource(id = R.drawable.mitch)
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned {
                                        capturingViewBounds = it.boundsInRoot()
                                    }
                                ,
                                contentScale = ContentScale.FillWidth,
                                painter = image,
                                contentDescription = ""
                            )
                            assetList.forEach {
                                RenderAsset(
                                    assetData = it,
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
                                    onGesture = {
                                        // TODO("CONTINUE HERE")
                                        /**
                                         * This doesn't work b/c if the object doesn't move we don't capture any bitmap.
                                         * So need to create some kind of a runnable job that starts when the recording starts
                                         * and capture a bitmap every timeInterval.
                                         */
                                        if (isRecording) {
                                            val delta = System.currentTimeMillis() - previousCaptureTime
                                            if (totalCaptureTime > 5000) {
                                                isRecording = false // force stop
                                                resetRecording()
                                            }
                                            if (delta > 250) {
                                                totalCaptureTime += delta.toInt()
                                                previousCaptureTime = System.currentTimeMillis()
                                                captureBitmap(capturingViewBounds, view)?.let { bmp ->
                                                    Log.d(TAG, "Capture bitmap...")
                                                    capturedBitmaps.add(bmp)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val capturedBitmaps: MutableList<Bitmap> = mutableListOf()

    private fun captureBitmap(capturingViewBounds: Rect?, view: View): Bitmap? {
        val bounds = capturingViewBounds ?: return null // TODO("Show error of some kind in UI?")
        val bmp = Bitmap.createBitmap(
            bounds.width.roundToInt(), bounds.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-bounds.left, -bounds.top)
            view.draw(this)
        }
        return bmp
    }

    private fun saveBitmaps(bitmaps: List<Bitmap>) {
        // TODO("figure out how I'm gunna do this...")
//        // If API >= 29 we can use scoped storage and don't require permission to save images.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            saveFileToScopedStorage("screenshot", bmp)
//        } else {
//            // Scoped storage doesn't exist before Android 29 so need to check permissions
//            if (checkPermissions()) {
//                saveFileToStorage("screenshot", bmp)
//            } else {
//                externalStoragePermissionRequest.launch(
//                    arrayOf(
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                    )
//                )
//            }
//        }
    }
}

private val assetList: MutableList<AssetData> = mutableListOf(
    AssetData(
        id = R.drawable.deal_with_it_sunglasses_default,
        initialOffset = Offset(0f, 0f),
        initialSize = Size(200f, 200f)
    )
)

data class AssetData(
    @DrawableRes val id: Int,
    val initialOffset: Offset,
    val initialSize: Size,
)

@Composable
fun RenderAsset(
    assetData: AssetData,
    isEnabled: Boolean,
    onGestureStarted: () -> Unit,
    onGestureFinished: () -> Unit,
    onGesture: () -> Unit,
) {
    var offset by remember { mutableStateOf(assetData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(assetData.initialSize) }

    // Not sure why I couldn't just use isEnabled but it doesn't work somehow.
    var enabled by remember { mutableStateOf(isEnabled) }
    if (enabled != isEnabled) {
        enabled = isEnabled
    }
    val xOffset = with(LocalDensity.current) { -offset.x.toDp() }
    val yOffset = with(LocalDensity.current) { -offset.y.toDp() }
    Image(
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
                        onGestureStarted()
                    },
                    onGestureFinished = {
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
                        onGesture()
                    }
                )
            }
            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin.Center
            }
            .clip(RectangleShape)
        ,
        painter = painterResource(id = assetData.id),
        contentDescription = ""
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

