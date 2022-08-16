package com.codingwithmitch.giffit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.ui.theme.GiffitTheme

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
                        val offsetXDp = with(LocalDensity.current) { offset.x.toDp() }
                        val offsetYDp = with(LocalDensity.current) { offset.y.toDp() }
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(50.dp)
                                .offset(
                                    x = offsetXDp,
                                    y = offsetYDp
                                )
                                .clip(RectangleShape)
                                .background(Color.Black)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        offset += dragAmount
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
