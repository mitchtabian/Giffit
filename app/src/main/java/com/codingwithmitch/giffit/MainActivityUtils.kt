package com.codingwithmitch.giffit

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options

fun MainActivity.buildBackgroundAssetPickerLauncher(
    onSuccess: (Uri) -> Unit,
    onFailure: () -> Unit,
) = registerForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        buildCropAssetLauncher(
            onSuccess = onSuccess,
            onFailure = onFailure
        ).launch(
            options(uri = it) {
                setGuidelines(CropImageView.Guidelines.ON)
            }
        )
    } ?: onFailure()
}

private fun MainActivity.buildCropAssetLauncher(
    onSuccess: (Uri) -> Unit,
    onFailure: () -> Unit,
) = registerForActivityResult(
    CropImageContract()
) { result ->
    if (result.isSuccessful) {
        result.uriContent?.let(onSuccess) ?: onFailure()
    } else {
        onFailure()
    }
}