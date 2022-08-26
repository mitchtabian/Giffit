package com.codingwithmitch.giffit.ui

import android.Manifest
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.codingwithmitch.giffit.domain.util.BitmapUtils.checkFilePermissions
import com.codingwithmitch.giffit.ui.MainState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.ui.compose.ui.GiffitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    private fun launchPermissionRequest() {
        externalStoragePermissionRequest.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

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
            result.uriContent?.let {
                when(val state = viewModel.state.value) {
                    is DisplaySelectBackgroundAsset,
                    is DisplayBackgroundAsset -> {
                        viewModel.state.value = DisplayBackgroundAsset(
                            backgroundAssetUri = it,
                        )
                    }
                    else -> throw Exception("Invalid state: $state")
                }
            }
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

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO("Audit this. Correct way to observe this?")
        viewModel.toastEventRelay.onEach { toastEvent ->
            if (toastEvent != null) {
                Toast.makeText(this@MainActivity, toastEvent.message, Toast.LENGTH_LONG).show()
            }
        }.launchIn(lifecycleScope)

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
                    val state = viewModel.state.value
                    val mainLoadingState = viewModel.loadingState.value
                    Column(modifier = Modifier.fillMaxSize()) {
                        when(state) {
                            Initial -> {
                                LoadingUI(mainLoadingState = MainLoadingState.Standard(Active()))
                                viewModel.state.value = DisplaySelectBackgroundAsset(backgroundAssetPickerLauncher)
                            }
                            is DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                backgroundAssetPickerLauncher = backgroundAssetPickerLauncher
                            )
                            is DisplayGif -> Gif(
                                imageLoader = imageLoader,
                                gifUri = state.resizedGifUri ?: state.gifUri,
                                discardGif = viewModel::deleteGif,
                                isResizedGif = state.resizedGifUri != null,
                                resetGifToOriginal = viewModel::resetGifToOriginal,
                                onSavedGif = {
                                    viewModel.saveGif(
                                        contentResolver = contentResolver,
                                        launchPermissionRequest = {
                                            launchPermissionRequest()
                                        },
                                        checkFilePermissions = {
                                            checkFilePermissions()
                                        },
                                    )
                                },
                                currentGifSize = state.originalGifSize,
                                adjustedBytes = state.adjustedBytes,
                                updateAdjustedBytes = viewModel::updateAdjustedBytes,
                                sizePercentage = state.sizePercentage,
                                updateSizePercentage = viewModel::updateSizePercentage,
                                resizeGif = {
                                    viewModel.resizeGif(
                                        contentResolver = contentResolver,
                                    )
                                }
                            )
                            is DisplayBackgroundAsset -> {
                                val bitmapCaptureLoadingState = when (mainLoadingState) {
                                    is MainLoadingState.BitmapCapture -> mainLoadingState
                                    else -> null
                                }
                                BackgroundAsset(
                                    bitmapCaptureLoadingState = bitmapCaptureLoadingState,
                                    updateBitmapCaptureJobState = {
                                        viewModel.loadingState.value =
                                            MainLoadingState.BitmapCapture(it)
                                    },
                                    startBitmapCaptureJob = { view ->
                                        viewModel.runBitmapCaptureJob(
                                            contentResolver = contentResolver,
                                            capturingViewBounds = state.capturingViewBounds,
                                            window = window,
                                            view = view,
                                        )
                                    },
                                    backgroundAssetUri = state.backgroundAssetUri,
                                    assetData = state.assetData,
                                    updateCapturingViewBounds = { rect ->
                                        viewModel.state.value = state.copy(capturingViewBounds = rect)
                                    },
                                    bitmapCapture = mainLoadingState,
                                    launchImagePicker = {
                                        backgroundAssetPickerLauncher.launch("image/*")
                                    }
                                )
                            }
                        }
                    }
                    LoadingUI(mainLoadingState = mainLoadingState)
                    val errorEvents by viewModel.errorRelay.collectAsState()
                    ErrorEventHandler(
                        errorEvents = errorEvents,
                        onClearErrorEvents = viewModel::clearErrorEvents
                    )
                }
            }
        }
    }
}
