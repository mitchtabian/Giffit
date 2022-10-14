package com.codingwithmitch.giffit.ui.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * @param progress: value between 0..100 representing the progress of the resize job.
 */
@Composable
fun ResizingGifLoadingUI(
    gifResizingLoadingState: LoadingState
){
    if (gifResizingLoadingState is LoadingState.Active && gifResizingLoadingState.progress != null) {
        Log.d("TAG", "ResizingGifLoadingUI: RESIZING!!!")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(vertical = 12.dp),
                    text = "Resizing gif...",
                    style = MaterialTheme.typography.h5,
                    color = Color.White
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                    ,
                    progress = gifResizingLoadingState.progress,
                    backgroundColor = Color.White,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}