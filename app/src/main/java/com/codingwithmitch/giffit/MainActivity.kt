package com.codingwithmitch.giffit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.ui.theme.GiffitTheme

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
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        var offsetXPx by remember { mutableStateOf(0f) }
                        val offsetXDp = with(LocalDensity.current) { offsetXPx.toDp() }
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(50.dp)
                                .offset(
                                    x = offsetXDp,
                                    y = 50.dp
                                )
                                .clip(RectangleShape)
                                .background(Color.Black)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { deltaX ->
                                        offsetXPx += deltaX
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
