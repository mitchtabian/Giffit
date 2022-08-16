package com.codingwithmitch.giffit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
import java.lang.Math.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        var zoom by remember { mutableStateOf(1f) }
                        var angle by remember { mutableStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTransformGestures(
                                        onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                                            val oldScale = zoom
                                            val newScale = zoom * gestureZoom

                                            // For natural zooming and rotating, the centroid of the gesture should
                                            // be the fixed point where zooming and rotating occurs.
                                            // We compute where the centroid was (in the pre-transformed coordinate
                                            // space), and then compute where it will be after this delta.
                                            // We then compute what the new offset should be to keep the centroid
                                            // visually stationary for rotating and zooming, and also apply the pan.
                                            offset = (offset + centroid / oldScale).rotateBy(gestureRotate) -
                                                    (centroid / newScale + pan / oldScale)
                                            zoom = newScale
                                            angle += gestureRotate
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    translationX = -offset.x * zoom
                                    translationY = -offset.y * zoom
                                    scaleX = zoom
                                    scaleY = zoom
                                    rotationZ = angle
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                                .background(Color.Blue)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
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