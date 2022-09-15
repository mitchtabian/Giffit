package com.codingwithmitch.giffit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.ui.compose.BackgroundAsset
import com.codingwithmitch.giffit.ui.compose.SelectBackgroundAsset
import com.codingwithmitch.giffit.ui.compose.theme.GiffitTheme

class MainActivity : ComponentActivity() {

    private val _state: MutableState<MainState> = mutableStateOf(Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state = _state.value
                    Column(modifier = Modifier.fillMaxSize()) {
                        when(state) {
                            Initial -> {
                                // TODO("Show loading UI")
                                _state.value = DisplaySelectBackgroundAsset(
                                    backgroundAssetPickerLauncher = buildBackgroundAssetPickerLauncher(
                                        onSuccess = {
                                            when(state) {
                                                is DisplaySelectBackgroundAsset -> {
                                                    _state.value = DisplayBackgroundAsset(
                                                        backgroundAssetUri = it,
                                                        backgroundAssetPickerLauncher = state.backgroundAssetPickerLauncher
                                                    )
                                                }
                                                is DisplayBackgroundAsset -> {
                                                    _state.value = state.copy(backgroundAssetUri = it)
                                                }
                                                else -> throw Exception("Invalid state: $state")
                                            }
                                        },
                                        onFailure = {
                                            Toast.makeText(this@MainActivity, "Something went wrong when selecting the image.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                )
                            }
                            is DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                launchImagePicker = {
                                    state.backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )
                            is DisplayBackgroundAsset -> BackgroundAsset(
                                backgroundAssetUri = state.backgroundAssetUri,
                                launchImagePicker = {
                                    state.backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

