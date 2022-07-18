package com.codingwithmitch.giffit

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.codingwithmitch.giffit.BitmapUtils.checkFilePermissions
import com.codingwithmitch.giffit.BitmapUtils.discardGif
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.ui.theme.GiffitTheme

@OptIn(ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {

    private lateinit var imageLoader: ImageLoader

    private val externalStoragePermissionRequest = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                Toast.makeText(this@MainActivity, "To enable this permission you'll have to do so in system settings for this app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> = this@MainActivity.registerForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            viewModel.backgroundAsset.value = result.uriContent
        } else {
            Toast.makeText(this@MainActivity, "Something went wrong cropping the image.", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        cropAssetLauncher.launch(
            options(
                uri = it,
            ) {
                setGuidelines(CropImageView.Guidelines.ON)
            }
        )
    }

    private val viewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!::imageLoader.isInitialized) {
            imageLoader = ImageLoader.Builder(this)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        // Scoped storage doesn't exist before Android 29 so need to check permissions
        if (SDK_INT < Build.VERSION_CODES.Q) {
            if (!checkFilePermissions()) {
                externalStoragePermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    )
                )
            }
        }
        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (viewModel.gifResizeProgress.value > 0) {
                        ResizingGifProgressBar(viewModel.gifResizeProgress.value)
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val view = LocalView.current
                            if (viewModel.gifUri.value != null || viewModel.isBuildingGif.value) {
                                Log.d(TAG, "Render: isBuildingGif: ${viewModel.isBuildingGif}")
                                DisplayGif(
                                    gifUri = viewModel.gifUri.value,
                                    imageLoader = imageLoader,
                                    isBuildingGif = viewModel.isBuildingGif.value,
                                    discardGif = {
                                        viewModel.gifUri.value?.let {
                                            discardGif(it)
                                            viewModel.gifUri.value= null
                                        }
                                    },
                                    onSavedGif = {
                                        viewModel.gifUri.value = null
                                        Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                                    },
                                    currentGifSize = viewModel.gifSize.value,
                                    adjustedBytes = viewModel.adjustedBytes.value,
                                    updateAdjustedBytes = {
                                        viewModel.adjustedBytes.value = it
                                    },
                                    sizePercentage = viewModel.sizePercentage.value,
                                    updateSizePercentage = {
                                        viewModel.sizePercentage.value = it
                                    },
                                    resizeGif = {
                                        viewModel.resizeGif(
                                            context = this@MainActivity,
                                            contentResolver = contentResolver
                                        )
                                    }
                                )
                            } else {
                                if (viewModel.backgroundAsset.value != null) {
                                    RecordButton(
                                        isRecording = viewModel.bitmapCaptureJobState.value == BitmapCaptureJobState.Running,
                                        updateBitmapCaptureJobState = { state ->
                                            Log.d(TAG, "Update job state: ${state}")
                                            viewModel.bitmapCaptureJobState.value = state
                                        },
                                        startBitmapCaptureJob = {
                                            viewModel.runBitmapCaptureJob(
                                                capturingViewBounds = viewModel.capturingViewBounds.value,
                                                window = window,
                                                view = view,
                                                sizePercentage = 1f, // TODO("REmove this.")
                                                onRecordingComplete = {
                                                    viewModel.buildGif(
                                                        context = this@MainActivity,
                                                        onSaved = {
                                                            viewModel.gifUri.value = it
                                                            viewModel.getGifSize(
                                                                contentResolver = contentResolver,
                                                                uri = it
                                                            )
                                                        },
                                                        launchPermissionRequest = {
                                                            externalStoragePermissionRequest.launch(
                                                                arrayOf(
                                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        },
                                    )
                                    RenderBackground(
                                        backgroundAsset = viewModel.backgroundAsset.value,
                                        assetData = viewModel.assetData.value,
                                        updateCapturingViewBounds = {
                                            viewModel.capturingViewBounds.value = it
                                        }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Button(
                                            modifier = Modifier
                                                .align(Alignment.Center),
                                            onClick = {
                                                backgroundAssetPickerLauncher.launch("image/*")
                                            }
                                        ) {
                                            Text("Choose background image")
                                        }
                                    }
                                }
                                Footer(
                                    backgroundAsset = viewModel.backgroundAsset.value,
                                    isRecording = viewModel.bitmapCaptureJobState.value == BitmapCaptureJobState.Running,
                                ) {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
