package com.codingwithmitch.giffit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
                    // Option 1: Boxes
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(50.dp)
                                .clip(RectangleShape)
                                .background(Color.Blue)
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(50.dp)
                                .offset(
                                    x = 50.dp,
                                    y = 50.dp
                                )
                                .clip(RectangleShape)
                                .background(Color.Black)
                        )
                    }

                    // Option 2: Canvas
//                    Canvas(
//                        modifier = Modifier.fillMaxSize()
//                    ) {
//                        drawRect(
//                            color = Color.Blue,
//                            size = Size(50.dp.toPx(), 50.dp.toPx()),
//                            topLeft = Offset(
//                                x = 200.dp.toPx(),
//                                y = 200.dp.toPx()
//                            )
//                        )
//                        drawRect(
//                            color = Color.Black,
//                            size = Size(50.dp.toPx(), 50.dp.toPx()),
//                            topLeft = Offset(
//                                x = 50.dp.toPx(),
//                                y = 50.dp.toPx()
//                            )
//                        )
//                    }
                }
            }
        }
    }
}
