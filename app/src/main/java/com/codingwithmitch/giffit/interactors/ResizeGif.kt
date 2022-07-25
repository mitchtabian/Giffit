package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.BitmapUtils
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.*

/**
 * TODO("ktdoc")
 */
class ResizeGif(
    private val buildGif: BuildGif,
    private val getAssetSize: GetAssetSize,
) {

    sealed class GifResizeResult {

        data class Finished(
            val uri: Uri,
        ): GifResizeResult()

        data class Continue(
            val uri: Uri,
            val percentageLoss: Float,
        ): GifResizeResult()
    }

    fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        previousUri: Uri?,
        percentageLoss: Float,
        discardCachedGif: (Uri) -> Unit,
    ): Flow<DataState<out GifResizeResult>> = flow {
        // If this is the first iteration of resizing, show loading right away.
        if (percentageLoss <= percentageLossIncrementSize)  {
            emit(Loading<GifResizeResult>(Active(percentageLoss)))
        }
        try {
           emitAll(
               resize(
                   contentResolver = contentResolver,
                   capturedBitmaps = capturedBitmaps,
                   originalGifSize = originalGifSize,
                   previousUri = previousUri,
                   targetSize = targetSize,
                   percentageLoss = percentageLoss,
                   discardCachedGif = discardCachedGif
               )
           )
        } catch (e: Exception) {
            Log.e(TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: RESIZE_GIF_ERROR))
        }
    }

    private fun resize(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        previousUri: Uri?,
        targetSize: Float,
        percentageLoss: Float,
        discardCachedGif: (Uri) -> Unit,
    ): Flow<DataState<out GifResizeResult>> = flow {
        // Delete the previously resized gif since we're moving to the next iteration.
        previousUri?.let {
            try {
                discardCachedGif(it)
            } catch (e: Exception) {
                Log.e(TAG, "GetAssetSize: ", e)
                throw Exception(RESIZE_GIF_ERROR)
            }
        }
        val resizedBitmaps: MutableList<Bitmap> = mutableListOf()
        for (bitmap in capturedBitmaps) {
            val resizedBitmap = BitmapUtils.resizeBitmap(
                bitmap = bitmap,
                sizePercentage = 1 - percentageLoss
            )
            resizedBitmaps.add(resizedBitmap)
        }

        emitAll(
            buildGif.execute(
                contentResolver = contentResolver,
                bitmaps = resizedBitmaps,
            ).transform { dataState ->
                when(dataState) {
                    is Data -> {
                        emitAll(
                            getAssetSize.execute(
                                contentResolver = contentResolver,
                                uri = dataState.data,
                            ).transform { dataState2 ->
                                when(dataState2) {
                                    is Data -> {
                                        val newSize = dataState2.data ?: 0
                                        val progress = (originalGifSize - newSize.toFloat()) / (originalGifSize - targetSize)
                                        emit(Loading<GifResizeResult>(Active(progress)))

                                        if (newSize > targetSize) {
                                            emit(
                                                dataState.data?.let { uri ->
                                                    Data(
                                                        GifResizeResult.Continue(
                                                            uri = uri,
                                                            percentageLoss = percentageLoss + percentageLossIncrementSize
                                                        )
                                                    )
                                                } ?: Error(RESIZE_GIF_ERROR)
                                            )
                                        } else {
                                            // Done resizing
                                            emit(
                                                dataState.data?.let { uri ->
                                                    Data(GifResizeResult.Finished(uri))
                                                } ?: Error(RESIZE_GIF_ERROR)
                                            )
                                            emit(Loading(Idle))
                                        }
                                    }
                                    is Error -> {
                                        emit(Error(dataState2.message))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    companion object {
        const val RESIZE_GIF_ERROR = "An error occurred while resizing the gif."
        /**
         * How much the gif gets resized after each iteration.
         * 0.05 = 5%.
         */
        const val percentageLossIncrementSize = 0.05f
    }
}