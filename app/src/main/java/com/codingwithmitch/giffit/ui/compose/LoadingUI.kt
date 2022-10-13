package com.codingwithmitch.giffit.ui.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.codingwithmitch.giffit.domain.DataState.Loading.*

@Composable
fun StandardLoadingUI(
    loadingState: LoadingState,
) {
    when(loadingState) {
        is LoadingState.Active -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(3f)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = Color.Blue,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}