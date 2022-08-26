package com.codingwithmitch.giffit.ui.compose

import android.net.Uri
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.codingwithmitch.giffit.domain.util.AssetData
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.ui.MainLoadingState
import kotlin.math.PI
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
fun RenderAsset(
    assetData: AssetData,
    screenWidthDp: Int,
    assetContainerHeightDp: Int,
) {
    var offset by remember { mutableStateOf(assetData.initialOffset) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }

    Box (
        modifier = Modifier
            .width(screenWidthDp.dp)
            .height(assetContainerHeightDp.dp)
    ) {
        val painter: Painter = rememberAsyncImagePainter(model = assetData.id)
        Image(
            modifier = Modifier
                .graphicsLayer {
                    val rotatedOffset = offset.rotateBy(angle)
                    translationX = -rotatedOffset.x
                    translationY = -rotatedOffset.y
                    scaleX = zoom
                    scaleY = zoom
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .pointerInput(Unit) {
                    // Shout-out to the dude who posted on issue tracker https://issuetracker.google.com/issues/233251825
                    detectTransformGestures(
                        onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                            val oldScale = zoom
                            val newScale = zoom * gestureZoom
                            angle += gestureRotate
                            zoom = newScale
                            offset = (offset - centroid * oldScale).rotateBy(-gestureRotate) +
                                    (centroid * newScale - pan * oldScale)
                        }
                    )
                }
                .size(
                    width = assetData.initialSize.width.dp,
                    height = assetData.initialSize.height.dp
                )
                .zIndex(1f)
            ,
            painter = painter,
            contentDescription = ""
        )
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