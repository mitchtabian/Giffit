package com.codingwithmitch.giffit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.ui.compose.BackgroundAsset
import com.codingwithmitch.giffit.ui.compose.Gif
import com.codingwithmitch.giffit.ui.compose.SelectBackgroundAsset
import com.codingwithmitch.giffit.ui.compose.theme.GiffitTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imageLoader: ImageLoader

    fun checkFilePermissions(): Boolean  {
        val writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    private val externalStoragePermissionRequest = this@MainActivity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                viewModel.showToast(message = "To enable this permission you'll have to do so in system settings for this app.")
            }
        }
    }

    private fun launchPermissionRequest() {
        externalStoragePermissionRequest.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> = this@MainActivity.registerForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let {
                when(val state = viewModel.state.value) {
                    is DisplaySelectBackgroundAsset,
                    is DisplayBackgroundAsset -> {
                        viewModel.updateState(
                            DisplayBackgroundAsset(
                                backgroundAssetUri = it,
                                capturingViewBounds = null,
                            )
                        )
                    }
                    else -> throw Exception("Invalid state: $state")
                }
            }
        } else {
            viewModel.showToast(message = "Something went wrong cropping the image.")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO("Will remove this when we add Hilt for DI.")
        imageLoader = ImageLoader.Builder(application)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        // TODO("Will remove this when we add Hilt for DI.")
        viewModel.setCacheProvider(RealCacheProvider(application))

        viewModel.toastEventRelay.onEach { toastEvent ->
            if (toastEvent != null) {
                Toast.makeText(this@MainActivity, toastEvent.message, Toast.LENGTH_LONG).show()
            }
        }.launchIn(lifecycleScope)

        setContent {
            GiffitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state = viewModel.state.value
                    val view = LocalView.current
                    Column(modifier = Modifier.fillMaxSize()) {
                        when(state) {
                            Initial -> {
                                // TODO("Show loading UI")
                                viewModel.updateState(
                                    DisplaySelectBackgroundAsset
                                )
                            }
                            is DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )
                            is DisplayBackgroundAsset -> BackgroundAsset(
                                backgroundAssetUri = state.backgroundAssetUri,
                                updateCapturingViewBounds = { rect ->
                                    viewModel.updateState(
                                        state.copy(capturingViewBounds = rect)
                                    )
                                },
                                startBitmapCaptureJob = {
                                    viewModel.runBitmapCaptureJob(
                                        contentResolver = contentResolver,
                                        view = view,
                                        window = window
                                    )
                                },
                                endBitmapCaptureJob = viewModel::endBitmapCaptureJob,
                                bitmapCaptureLoadingState = state.bitmapCaptureLoadingState,
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                },
                                loadingState = state.loadingState
                            )
                            is DisplayGif -> Gif(
                                imageLoader = imageLoader,
                                gifUri = state.gifUri,
                                discardGif = viewModel::deleteGif,
                                onSavedGif = {
                                    viewModel.saveGif(
                                        context = this@MainActivity,
                                        contentResolver = contentResolver,
                                        launchPermissionRequest = {
                                            launchPermissionRequest()
                                        },
                                        checkFilePermissions = ::checkFilePermissions,
                                    )
                                },
                                currentGifSize = state.originalGifSize,
                                adjustedBytes = state.adjustedBytes,
                                isResizedGif = state.resizedGifUri != null,
                                resetGifToOriginal = {
                                    // TODO("delegate to viewmodel")
                                },
                                updateAdjustedBytes = {
                                    // TODO("delegate to viewmodel")
                                },
                                sizePercentage = state.sizePercentage,
                                updateSizePercentage = {
                                    // TODO("delegate to viewmodel")
                                },
                                resizeGif = {
                                    // TODO("delegate to viewmodel")
                                },
                                loadingState = state.loadingState,
                                gifResizingLoadingState = state.resizeGifLoadingState
                            )
                        }
                    }
                }
            }
        }
    }
}

