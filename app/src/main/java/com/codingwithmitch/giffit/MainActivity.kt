package com.codingwithmitch.giffit

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getContentUri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import kotlin.math.*


val TAG = "MitchsLog"

class MainActivity : ComponentActivity() {

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

    private var isBitmapCaptureJobRunning = mutableStateOf(false)

    private val capturedBitmaps: MutableList<Bitmap> = mutableListOf()

    private val assetData: MutableState<AssetData?> = mutableStateOf(
        AssetData(
            id = R.drawable.deal_with_it_sunglasses_default,
            initialOffset = Offset(0f, 0f),
            initialSize = Size(200f, 200f)
        )
    )

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
                        var isRecording by remember { isBitmapCaptureJobRunning }
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .align(Alignment.End)
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
                                isRecording = !isRecording
                                if (isRecording) { // Start recording
                                    runBitmapCaptureJob(capturingViewBounds, view)
                                } else { // End recording
                                    isRecording = false
                                }
                            }
                        ) {
                            Text(
                                text = if (isRecording) {
                                    "End"
                                } else {
                                    "Start"
                                }
                            )
                        }
                        var assetData by remember { assetData }
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
                            assetData?.let { RenderAsset(it) }
                        }
                    }
                }
            }
        }
    }

    private fun runBitmapCaptureJob(capturingViewBounds: Rect?, view: View) {
        if (capturingViewBounds == null) throw Exception("Invalid capture area.")
        CoroutineScope(Main).launch {
            var elapsedTime = 0
            while (elapsedTime < 3000 && isBitmapCaptureJobRunning.value) {
                delay(250)
                elapsedTime += 250
                captureBitmap(capturingViewBounds, view)?.let { bmp ->
                    Log.d(TAG, "Capture bitmap...")
                    capturedBitmaps.add(bmp)
                }
            }
            isBitmapCaptureJobRunning.value = false
            buildGifFromBitmaps()
        }
    }

    private fun buildGifFromBitmaps() {
        val writer = AnimatedGIFWriter(true)
        val bos = ByteArrayOutputStream()
        writer.prepareForWrite(bos, -1, -1)
        for(bitmap in capturedBitmaps) {
            writer.writeFrame(bos, bitmap)
        }
        writer.finishWrite(bos)
        saveGif("animated", bos.toByteArray())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveGifToScopedStorage(displayName: String, bytes: ByteArray) {
        try {
            val externalUri: Uri = getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            // Add content values so media is discoverable by android and added to common directories.
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.gif")
            contentResolver.insert(externalUri, contentValues)?.let { fileUri ->
                contentResolver.openOutputStream(fileUri)?.let { os ->
                    os.write(bytes)
                    os.flush()
                    os.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        capturedBitmaps.clear()
    }

    private fun saveGifToStorage(displayName: String, bytes: ByteArray) {
        try {
            // Add content values so media is discoverable by android and added to common directories.
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.let { os ->
                    os.write(bytes)
                    os.flush()
                    os.close()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        capturedBitmaps.clear()
    }

    private fun saveGif(displayName: String, bytes: ByteArray) {
        // If API >= 29 we can use scoped storage and don't require permission to save images.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveGifToScopedStorage(displayName, bytes)
        } else {
            // Scoped storage doesn't exist before Android 29 so need to check permissions
            if (checkPermissions()) {
                saveGifToStorage(displayName, bytes)
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

    private fun captureBitmap(capturingViewBounds: Rect?, view: View): Bitmap? {
        val bounds = capturingViewBounds ?: return null
        val bmp = Bitmap.createBitmap(
            bounds.width.roundToInt(), bounds.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            translate(-bounds.left, -bounds.top)
            view.draw(this)
        }
        return bmp
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
        painter = painterResource(id = assetData.id),
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

