package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.BitmapUtils
import com.codingwithmitch.giffit.BitmapUtils.discardGif
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ResizeGif(
    private val buildGif: BuildGif,
    private val getAssetSize: GetAssetSize,
    private val ioScope: CoroutineScope,
) {

    /**
     * How much the gif gets resized after each iteration.
     * 0.05 = 5%.
     */
    private val percentageLossIncrementSize = 0.05f


    fun test(): Flow<String> = flow {
        emitAll(
            secondFlow().transform {
                emit(it)
            }
        )
    }

    fun secondFlow(): Flow<String> = flow {
        (0..5).forEach {
            emitAll(
                thirdFlow(it).transform { intValue ->
                    emit(intValue.toString())
                }
            )
        }
    }

    fun thirdFlow(input: Int): Flow<Int> = flow {
        emit(input)
    }

    fun execute(
        context: Context,
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        launchPermissionRequest: () -> Unit
    ): Flow<DataState<Uri>> = flow {
        emit(Loading<Uri>(Active(percentageLossIncrementSize)))
        try {
           emitAll(
               resize(
                   context = context,
                   contentResolver = contentResolver,
                   capturedBitmaps = capturedBitmaps,
                   originalGifSize = originalGifSize,
                   previousUri = null,
                   targetSize = targetSize,
                   percentageLoss = percentageLossIncrementSize,
                   launchPermissionRequest = launchPermissionRequest,
               )
           )
        } catch (e: Exception) {
            emit(Error(e.message ?: RESIZE_GIF_ERROR))
        }


//        resize(
//            context = context,
//            contentResolver = contentResolver,
//            capturedBitmaps = capturedBitmaps,
//            originalGifSize = originalGifSize,
//            previousUri = null,
//            targetSize = targetSize,
//            percentageLoss = percentageLossIncrementSize,
//            launchPermissionRequest = launchPermissionRequest,
////            onProgressUpdate = { progress ->
////                emit(Loading<Uri>(Active(progress)))
////            },
////            onError = {
////                emit(Error<Uri>(it))
////            },
////            onResizeComplete = { uri ->
////                emit(Data(uri))
////            }
//        )
    }

    private fun resize(
        context: Context,
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        previousUri: Uri?,
        targetSize: Float,
        percentageLoss: Float,
        launchPermissionRequest: () -> Unit,
//        onProgressUpdate: suspend (Float) -> Unit,
//        onError: suspend (String) -> Unit,
//        onResizeComplete: (Uri) -> Unit,
    ): Flow<DataState<Uri>> = flow {
        previousUri?.let {
            try {
                context.discardGif(it)
            } catch (e: Exception) {
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
                context = context,
                bitmaps = resizedBitmaps,
                launchPermissionRequest = launchPermissionRequest,
            ).transform { dataState ->
                when(dataState) {
                    is Data -> {
                        emitAll(
                            getAssetSize.execute(
                                contentResolver = contentResolver,
                                uri = dataState.data,
                            ).transform { dataState2 ->
                                when(dataState2) {
                                    is Data -> { // TODO("Need error case")
                                        val newSize = dataState2.data ?: 0
                                        val progress = (originalGifSize - newSize.toFloat()) / (originalGifSize - targetSize)
                                        emit(Loading(Active(progress)))

                                        // If we haven't reached the target size, recursively call until we do.
                                        if (newSize > targetSize) {
                                            emitAll(
                                                resize(
                                                    context = context,
                                                    contentResolver = contentResolver,
                                                    capturedBitmaps = capturedBitmaps,
                                                    originalGifSize = originalGifSize,
                                                    targetSize = targetSize,
                                                    previousUri = dataState.data,
                                                    percentageLoss = percentageLoss + percentageLossIncrementSize,
                                                    launchPermissionRequest = launchPermissionRequest,
                                                )
                                            )
                                        } else {
                                            // Done resizing
                                            emit(Data(dataState.data))
                                            emit(Loading<Uri>(Idle))
                                        }
                                    }
                                    is Error -> {
                                        emit(Error<Uri>(dataState2.message))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )


//        buildGif.execute(
//            context = context,
//            bitmaps = resizedBitmaps,
//            onSaved = {
//                getAssetSize.execute(
//                    contentResolver = contentResolver,
//                    uri = it,
//                ).onEach { dataState ->
//                    when(dataState) {
//                        is Data -> {
//                            val newSize = dataState.data ?: 0
//                            val progress = (originalGifSize - newSize.toFloat()) / (originalGifSize - targetSize)
////                            emit(Loading(Active(progress)))
//
//                            // If we haven't reached the target size, recursively call until we do.
//                            if (newSize > targetSize) {
//                                resize(
//                                    context = context,
//                                    contentResolver = contentResolver,
//                                    capturedBitmaps = capturedBitmaps,
//                                    originalGifSize = originalGifSize,
//                                    targetSize = targetSize,
//                                    previousUri = it,
//                                    percentageLoss = percentageLoss + percentageLossIncrementSize,
//                                    launchPermissionRequest = launchPermissionRequest,
////                                    onProgressUpdate = onProgressUpdate,
////                                    onError = onError,
////                                    onResizeComplete = onResizeComplete
//                                )
//                            } else {
//                                // Done resizing
////                                emit(Data(it))
////                                emit(Loading<Uri>(Idle))
////                                onResizeComplete(it)
////                                onProgressUpdate(0f)
//                            }
//                        }
//                        is Error -> {
////                            emit(Error<Uri>(dataState.message))
////                            onError(dataState.message)
//                        }
//                    }
//                }.launchIn(ioScope)
//            },
//            launchPermissionRequest = launchPermissionRequest
//        ).onEach { dataState ->
//            when(dataState) {
//                is Error -> {
////                    emit(Error<Uri>(dataState.message))
////                    onError(dataState.message)
//                }
//            }
//        }.launchIn(ioScope)
    }

    companion object {
        const val RESIZE_GIF_ERROR = "An error occurred while resizing the gif."
    }
}