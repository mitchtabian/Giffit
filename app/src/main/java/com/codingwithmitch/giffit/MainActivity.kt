package com.codingwithmitch.giffit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.codingwithmitch.giffit.ui.compose.RenderBackground
import com.codingwithmitch.giffit.ui.compose.theme.GiffitTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // Gif capture area
                    val configuration = LocalConfiguration.current
                    val assetContainerHeight = remember { (configuration.screenHeightDp * 0.6).toInt() }
                    RenderBackground(
                        assetContainerHeightDp = assetContainerHeight
                    )
                }
            }
        }
    }
}

