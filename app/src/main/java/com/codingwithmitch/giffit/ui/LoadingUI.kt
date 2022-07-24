package com.codingwithmitch.giffit.ui

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
import com.codingwithmitch.giffit.MainViewModel
import com.codingwithmitch.giffit.domain.DataState

@Composable
fun LoadingUI(
    mainLoadingState: MainViewModel.MainLoadingState,
) {
    when(mainLoadingState) {
        is MainViewModel.MainLoadingState.Standard -> {
            when(mainLoadingState.loadingState) {
                is DataState.Loading.LoadingState.Active -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
        is MainViewModel.MainLoadingState.ResizingGif -> {
            when(val loadingState = mainLoadingState.loadingState) {
                is DataState.Loading.LoadingState.Active -> {
                    ResizingGifProgressBar(loadingState.progress ?: 0f)
                }
            }
        }
    }
}

/**
 * @param progress: value between 0..100 representing the progress of the resize job.
 */
@Composable
fun ResizingGifProgressBar(
    progress: Float,
){
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
                modifier = Modifier.align(Alignment.Start).padding(vertical = 12.dp),
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
                progress = progress,
                backgroundColor = Color.White,
                color = MaterialTheme.colors.primary
            )
        }
    }
}