package com.codingwithmitch.giffit

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.codingwithmitch.giffit.BitmapUtils.checkFilePermissions
import com.codingwithmitch.giffit.MainState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.ui.*
import com.codingwithmitch.giffit.ui.SelectBackgroundAsset
import com.codingwithmitch.giffit.ui.theme.GiffitTheme
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
        viewModel.toastEventRelay.onEach { message ->
            if (message != null) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
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
                    val mainLoadingState = viewModel.mainLoadingState.value
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
                                        viewModel.mainLoadingState.value = MainLoadingState.BitmapCapture(it)
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
                                    }
                                )
                            }
                        }
                        Footer(state, mainLoadingState) {
                            backgroundAssetPickerLauncher.launch("image/*")
                        }
                    }
                    LoadingUI(mainLoadingState = mainLoadingState)
                }
            }
        }
    }
}
