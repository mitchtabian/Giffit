package com.codingwithmitch.giffit.interactors

import android.net.Uri
import android.util.Log
import com.canhub.cropper.CropImageView
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Get the URI of the cropped image and it's size.
 */
class GetCroppedUriAndSize {

    data class CroppedImageData(
        val uri: Uri,
        val size: Int,
    )

    fun execute(
        result: CropImageView.CropResult,
        uncroppedImageSize: Int,
    ): Flow<DataState<CroppedImageData>> = flow {
        try {
            if (result.isSuccessful) {
                check(result.uriContent != null) { "Cropped URI cannot be null." }

                val croppedImageSize = estimateCroppedImageSize(
                    originalWidth = result.wholeImageRect?.width() ?: 0,
                    originalHeight = result.wholeImageRect?.height() ?: 0,
                    croppedWidth = result.cropRect?.width() ?: 0,
                    croppedHeight = result.cropRect?.height() ?: 0,
                    uncroppedImageSize = uncroppedImageSize
                )

                emit(
                    DataState.Data(
                        CroppedImageData(
                            uri = (result.uriContent as Uri),
                            size = croppedImageSize
                        )
                    )
                )
            } else {
                throw result.error ?: Exception(GET_CROPPED_URI_AND_SIZE_ERROR)
            }
        } catch (e: Exception) {
            emit(DataState.Error(e.message ?: GET_CROPPED_URI_AND_SIZE_ERROR))
        }
    }

    /**
     * Estimates the size of the cropped image by referencing the original uncropped
     *  image and multiplying by the area difference.
     *  @param originalWidth: Width of uncropped image.
     *  @param originalHeight: Height of uncropped image.
     *  @param croppedWidth: Width of cropped image.
     *  @param croppedHeight: Height of cropped image.
     *  @param uncroppedImageSize: Size (in bytes) of original uncropped image.
     */
    private fun estimateCroppedImageSize(
        originalWidth: Int,
        originalHeight: Int,
        croppedWidth: Int,
        croppedHeight: Int,
        uncroppedImageSize: Int,
    ): Int {
        // We can estimate the size of the cropped image using the difference in area.
        val area = originalHeight * originalWidth
        val croppedArea = croppedHeight * croppedWidth
        val deltaArea = area - croppedArea
        // How much % of the area was removed from the crop
        val pctAreaDelta = deltaArea.toFloat() / area.toFloat()
        return (uncroppedImageSize.toFloat() * (1 - pctAreaDelta)).toInt()
    }

    companion object {
        const val GET_CROPPED_URI_AND_SIZE_ERROR = "Something went wrong while trying to get the cropped URI and its size."
    }
}





