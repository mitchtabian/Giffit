package com.codingwithmitch.giffit.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.R
import java.lang.Math.*

@Composable
fun RenderBackground(
    assetContainerHeightDp: Int,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
    ) {
        val backgroundAsset = painterResource(R.drawable.mitch)
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(assetContainerHeightDp.dp)
            ,
            painter = backgroundAsset,
            contentScale = ContentScale.Crop,
            contentDescription = ""
        )
        RenderAsset(
            assetContainerHeightDp = assetContainerHeightDp
        )
    }
}

@Composable
fun RenderAsset(
    assetContainerHeightDp: Int
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var angle by remember { mutableStateOf(0f) }

    val asset = painterResource(R.drawable.deal_with_it_sunglasses_default)

    Box (
        modifier = Modifier
            .fillMaxWidth()
            .height(assetContainerHeightDp.dp)
    ) {
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
                .size(200.dp, 200.dp)
            ,
            painter = asset,
            contentDescription = ""
        )
    }
}

/**
 * Rotates the given offset around the origin by the given angle in degrees.
 *
 * A positive angle indicates a counterclockwise rotation around the right-handed 2D Cartesian
 * coordinate system.
 *
 * See: [Rotation matrix](https://en.wikipedia.org/wiki/Rotation_matrix)
 */
fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}