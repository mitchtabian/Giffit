package com.codingwithmitch.giffit.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codingwithmitch.giffit.ErrorEvent

@Composable
fun ErrorEventHandler(
    errorEvents: Set<ErrorEvent>,
    onClearErrorEvents: () -> Unit,
) {
    if (errorEvents.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.4f)
                    .shadow(elevation = 12.dp, RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                ,
                                text = "Error",
                                style = MaterialTheme.typography.h5
                            )
                        }
                        items(errorEvents.toList()) { errorEvent ->
                            Text(
                                modifier = Modifier.padding(bottom = 12.dp),
                                text = errorEvent.message
                            )
                        }
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    modifier = Modifier
                                        .align(Alignment.End),
                                    onClick = { onClearErrorEvents() }
                                ) {
                                    Text(
                                        text = "OK",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}