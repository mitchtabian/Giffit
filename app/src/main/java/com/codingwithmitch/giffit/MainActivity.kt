package com.codingwithmitch.giffit

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.codingwithmitch.giffit.ui.compose.BackgroundAsset
import com.codingwithmitch.giffit.ui.compose.theme.GiffitTheme

class MainActivity : ComponentActivity() {

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        backgroundAssetUri.value = it
        Log.d("TAG", "Got the URI: ${it}")
    }

    private val backgroundAssetUri: MutableState<Uri?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    BackgroundAsset(
                        launchImagePicker = {
                            backgroundAssetPickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}

