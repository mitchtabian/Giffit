package com.codingwithmitch.giffit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
    modalBottomSheetState: ModalBottomSheetState,
    drawableAssets: List<Int>,
    onSelectAsset: (Int) -> Unit,
) {
    ModalBottomSheetLayout(
        sheetBackgroundColor = Color.White,
        sheetState = modalBottomSheetState,
        sheetContent = {
            Divider(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 0.dp, vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(5.dp))
                    .shadow(16.dp, RoundedCornerShape(5.dp)),
                thickness = 4.dp,
                color = Color.LightGray
            )
            LazyVerticalGrid(
                cells = GridCells.Fixed(3)
            ) {
                items(drawableAssets) { drawable ->
                    AsyncImage(
                        modifier = Modifier.clickable {
                            onSelectAsset(drawable)
                        },
                        model = drawable,
                        contentDescription = ""
                    )
                }
            }
        }
    ) { }
}