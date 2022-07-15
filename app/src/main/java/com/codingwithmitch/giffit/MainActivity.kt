package com.codingwithmitch.giffit

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.applyCanvas
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.codingwithmitch.giffit.BitmapUtils.buildGifFromBitmapsAndSave
import com.codingwithmitch.giffit.BitmapUtils.checkFilePermissions
import com.codingwithmitch.giffit.BitmapUtils.discardGif
import com.codingwithmitch.giffit.BitmapUtils.estimateCroppedImageSize
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

val TAG = "MitchsLog"

@OptIn(ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {

    private lateinit var imageLoader: ImageLoader

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!::imageLoader.isInitialized) {
            imageLoader = ImageLoader.Builder(this)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        // Scoped storage doesn't exist before Android 29 so need to check permissions
        if (SDK_INT < Build.VERSION_CODES.Q) {
            if (!checkFilePermissions()) {
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
                        var capturingViewBounds by remember { mutableStateOf<Rect?>(null) }
                        var isRecording by remember { isBitmapCaptureJobRunning }
                        var backgroundAsset: Uri? by remember { mutableStateOf(null) }
                        var assetData by remember {
                            mutableStateOf(
                                AssetData(
                                    id = R.drawable.deal_with_it_sunglasses_default,
                                    initialOffset = Offset(0f, 0f),
                                    initialSize = Size(200f, 200f)
                                )
                            )
                        }
                        var gifUri: Uri? by remember { mutableStateOf(null) }
                        var isBuildingGif by remember { mutableStateOf(false) }
                        var adjustedBytes by remember { mutableStateOf(0) }
                        var sizePct by remember { mutableStateOf(100) }
                        var uncroppedImageSize by remember { mutableStateOf(0) }
                        var croppedImageSize by remember { mutableStateOf(0) }
                        val view = LocalView.current

                        val crop = rememberLauncherForActivityResult(
                            CropImageContract()
                        ) { result ->
                            if (result.isSuccessful) {
                                // use the returned uri
                                backgroundAsset = result.uriContent

                                croppedImageSize = estimateCroppedImageSize(
                                    originalWidth = result.wholeImageRect?.width() ?: 0,
                                    originalHeight = result.wholeImageRect?.height() ?: 0,
                                    croppedWidth = result.cropRect?.width() ?: 0,
                                    croppedHeight = result.cropRect?.height() ?: 0,
                                    uncroppedImageSize = uncroppedImageSize
                                )
                            } else {
                                // an error occurred
                                // TODO("Show error?")
                                val exception = result.error
                                Log.e(TAG, "Crop exception: ${exception}")
                            }
                        }
                        val backgroundAssetPicker = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null) {
                                contentResolver.openInputStream(uri)?.let {
                                    uncroppedImageSize = it.available()
                                    it.close()
                                }
                                crop.launch(
                                    options(
                                        uri = uri,
                                    ) {
                                        setGuidelines(CropImageView.Guidelines.ON)
                                    }
                                )
                            }
                        }
                        if (gifUri != null || isBuildingGif) {
                            DisplayGif(
                                gifUri = gifUri,
                                imageLoader = imageLoader,
                                isBuildingGif = isBuildingGif,
                                discardGif = {
                                    gifUri?.let {
                                        discardGif(it)
                                        gifUri = null
                                    }
                                }
                            ) {
                                gifUri = null
                                Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (backgroundAsset != null) {
                                RecordButton(
                                    isRecording = isRecording,
                                    updateBitmapCaptureJob = { isRunning ->
                                        isBitmapCaptureJobRunning.value = isRunning
                                    },
                                    startBitmapCaptureJob = {
                                        runBitmapCaptureJob(
                                            capturingViewBounds,
                                            view,
                                            sizePercentage = sizePct.toFloat() / 100,
                                            onRecordingComplete = {
                                                isBuildingGif = true
                                            }
                                        ) {
                                            isBuildingGif = false
                                            gifUri = it
                                            capturedBitmaps.clear()
                                        }
                                    },
                                )
                                RenderBackground(
                                    backgroundAsset = backgroundAsset,
                                    assetData = assetData,
                                    updateCapturingViewBounds = {
                                        capturingViewBounds = it
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Button(
                                        modifier = Modifier
                                            .align(Alignment.Center),
                                        onClick = {
                                            backgroundAssetPicker.launch("image/*")
                                        }
                                    ) {
                                        Text("Choose background image")
                                    }
                                }
                            }
                            Footer(
                                backgroundAsset = backgroundAsset,
                                isRecording = isRecording,
                                croppedImageSize = croppedImageSize,
                                adjustedBytes = adjustedBytes,
                                updateAdjustedBytes = {
                                    adjustedBytes = it
                                },
                                sizePercentage = sizePct,
                                updateSizePercentage = {
                                    sizePct = it
                                }
                            ) {
                                backgroundAssetPicker.launch("image/*")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param sizePercentage: 0.05..1
     */
    private fun runBitmapCaptureJob(
        capturingViewBounds: Rect?,
        view: View?,
        sizePercentage: Float,
        onRecordingComplete: () -> Unit,
        onSaved: (Uri) -> Unit
    ) {
        if (sizePercentage < 0.05 || sizePercentage > 1) throw Exception("Invalid resizing percentage.")
        if (capturingViewBounds == null) throw Exception("Invalid capture area.")
        if (view == null) throw Exception("Invalid view.")
        CoroutineScope(IO).launch {
            var elapsedTime = 0
            while (elapsedTime < 4000 && isBitmapCaptureJobRunning.value) {
                delay(250)
                elapsedTime += 250
                captureBitmap(
                    rect = capturingViewBounds,
                    view = view,
                    window = window,
                    sizePercentage = sizePercentage,
                ) {
                    // Prevent Concurrent modification exception depending on timing
                    if (isBitmapCaptureJobRunning.value) {
                        capturedBitmaps.add(it)
                    }
                }
            }
            isBitmapCaptureJobRunning.value = false
            onRecordingComplete()
            buildGifFromBitmapsAndSave(
                context = this@MainActivity,
                bitmaps = capturedBitmaps,
                onSaved = onSaved,
                launchPermissionRequest = {
                    externalStoragePermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                        )
                    )
                }
            )
        }
    }

    /**
     * @param sizePercentage: 0.05f..1
     */
    private fun resizeBitmap(bitmap: Bitmap, sizePercentage: Float): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }

    /**
     * @param sizePercentage: 0.05..1
     */
    private fun captureBitmap(
        rect: Rect?,
        view: View,
        window: Window,
        sizePercentage: Float,
        bitmapCallback: (Bitmap)->Unit
    ) {
        if (rect == null) return
        if (SDK_INT >= Build.VERSION_CODES.O) {

            val bitmap = Bitmap.createBitmap(
                view.width, view.height,
                Bitmap.Config.ARGB_8888
            )

            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            val xCoordinate = locationOfViewInWindow[0]
            val yCoordinate = locationOfViewInWindow[1]
            val scope = android.graphics.Rect(
                xCoordinate,
                yCoordinate,
                xCoordinate + view.width,
                yCoordinate + view.height
            )
            // Take screenshot
            PixelCopy.request(
                window,
                scope,
                bitmap,
                { p0 ->
                    if (p0 == PixelCopy.SUCCESS) {
                        // crop the screenshot
                        bitmapCallback.invoke(
                            resizeBitmap(
                                bitmap = Bitmap.createBitmap(
                                    bitmap,
                                    rect.left.toInt(),
                                    rect.top.toInt(),
                                    rect.width.roundToInt(),
                                    rect.height.roundToInt()
                                ),
                                sizePercentage = sizePercentage
                            )
                        )
                    }
                },
                Handler(Looper.getMainLooper()) )
        } else {
            val bitmap = resizeBitmap(
                bitmap = Bitmap.createBitmap(
                    rect.width.roundToInt(),
                    rect.height.roundToInt(),
                    Bitmap.Config.ARGB_8888
                ),
                sizePercentage = sizePercentage
            ).applyCanvas {
                translate(-rect.left, -rect.top)
                view.draw(this)
            }
            bitmapCallback.invoke(bitmap)
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

