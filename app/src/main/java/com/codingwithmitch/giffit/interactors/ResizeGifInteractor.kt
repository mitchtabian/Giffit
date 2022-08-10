package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.BitmapUtils
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * Interactor for resizing a gif.
 *
 * The process of resizing any kind of media is not easy. Unfortunately we can't just
 * call [Bitmap.createScaledBitmap] since you can't target a specific size. The only way
 * to do it accurately is to iteratively resize until you reach the target size.
 */
class ResizeGifInteractor
@Inject
constructor(
    private val versionProvider: VersionProvider,
    private val cacheProvider: CacheProvider,
) {

    fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean = true,
        discardCachedGif: (Uri) -> Unit,
    ): Flow<DataState<Uri>> = flow {
//        emit(
//            Error(
//                "Oppsie poopsie ${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}" +
//                        "${UUID.randomUUID()}"
//            )
//        )
        var previousUri: Uri? = null
        var progress: Float
        var percentageLoss = percentageLossIncrementSize
        emit(Loading<Uri>(Active(percentageLoss)))
        try {
            var resizing = true
            while (resizing) {
                // Delete the previously resized gif since we're moving to the next iteration.
                previousUri?.let {
                    try {
                        discardCachedGif(it)
                    } catch (e: Exception) {
                        Log.e(TAG, "ResizeGif: ", e)
                        throw Exception(e.message ?: RESIZE_GIF_ERROR)
                    }
                }
                val resizedBitmaps: MutableList<Bitmap> = mutableListOf()
                for (bitmap in capturedBitmaps) {
                    val resizedBitmap = BitmapUtils.resizeBitmap(
                        bitmap = bitmap,
                        sizePercentage = 1 - percentageLoss,
                        bilinearFiltering = bilinearFiltering
                    )
                    resizedBitmaps.add(resizedBitmap)
                }

                val result = BuildGifInteractor.buildGifAndSaveToInternalStorage(
                    contentResolver = contentResolver,
                    versionProvider = versionProvider,
                    cacheProvider = cacheProvider,
                    bitmaps = resizedBitmaps,
                )
                val newSize = result.gifSize.toFloat()
                val uri = result.uri
                progress = (originalGifSize - newSize) / (originalGifSize - targetSize)
                emit(Loading<Uri>(Active(progress)))

                // Continue to next iteration
                if (newSize > targetSize) {
                    previousUri = uri
                    percentageLoss += percentageLossIncrementSize
                } else {
                    // Done resizing
                    emit(Data(uri))
                    resizing = false
                }
            }
            emit(Loading(Idle))
        } catch (e: Exception) {
            Log.e(TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: RESIZE_GIF_ERROR))
        }
    }

    companion object {
        const val RESIZE_GIF_ERROR = "An error occurred while resizing the gif."
        /**
         * How much the gif gets resized after each iteration.
         * 0.05 = 5%.
         */
        private const val percentageLossIncrementSize = 0.05f
    }
}