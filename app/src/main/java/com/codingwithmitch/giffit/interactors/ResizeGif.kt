package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.interactors.ResizeGif.*
import com.codingwithmitch.giffit.interactors.util.GifUtil.buildGifAndSaveToInternalStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface ResizeGif {
    fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean = true,
        discardCachedGif: (Uri) -> Unit,
    ): Flow<DataState<ResizeGifResult>>

    /**
     * Result returned from [ResizeGif].
     * @param uri: [Uri] of the gif saved to internal storage.
     * @param gifSize: Size of the gif as it exists in internal storage.
     */
    data class ResizeGifResult(
        val uri: Uri,
        val gifSize: Int,
    )
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ResizeGifModule {
    @Binds
    abstract fun provideResizeGif(resizeGif: ResizeGifInteractor): ResizeGif
}

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
): ResizeGif {

    override fun execute(
        contentResolver: ContentResolver,
        capturedBitmaps: List<Bitmap>,
        originalGifSize: Float,
        targetSize: Float,
        bilinearFiltering: Boolean,
        discardCachedGif: (Uri) -> Unit,
    ): Flow<DataState<ResizeGifResult>> = flow {
        var previousUri: Uri? = null
        var progress: Float
        var percentageLoss = percentageLossIncrementSize
        emit(Loading(Active(percentageLoss)))
        try {
            var resizing = true
            while (resizing) {
                // Delete the previously resized gif since we're moving to the next iteration.
                previousUri?.let {
                    try {
                        discardCachedGif(it)
                    } catch (e: Exception) {
                        throw Exception(e.message ?: RESIZE_GIF_ERROR)
                    }
                }
                val resizedBitmaps: MutableList<Bitmap> = mutableListOf()
                for (bitmap in capturedBitmaps) {
                    val resizedBitmap = resizeBitmap(
                        bitmap = bitmap,
                        sizePercentage = 1 - percentageLoss,
                        bilinearFiltering = bilinearFiltering
                    )
                    resizedBitmaps.add(resizedBitmap)
                }

                val result = buildGifAndSaveToInternalStorage(
                    contentResolver = contentResolver,
                    versionProvider = versionProvider,
                    cacheProvider = cacheProvider,
                    bitmaps = resizedBitmaps,
                )
                val uri = result.uri
                val newSize = result.gifSize
                progress = (originalGifSize - newSize) / (originalGifSize - targetSize)
                emit(Loading(Active(progress)))

                // Continue to next iteration
                if (newSize > targetSize) {
                    previousUri = uri
                    percentageLoss += percentageLossIncrementSize
                } else {
                    // Done resizing
                    emit(Data(ResizeGifResult(uri = uri, gifSize = newSize.toInt())))
                    resizing = false
                }
            }
            emit(Loading(Idle))
        } catch (e: Exception) {
            emit(Error(e.message ?: RESIZE_GIF_ERROR))
        }
    }

    /**
     * @param bilinearFiltering: For some reason bilinear-filtering does some weird stuff in unit tests.
     *  It actually increases the size of the original bitmap. So I added it as a param so we can disable
     *  for unit tests.
     */
    private fun resizeBitmap(bitmap: Bitmap, sizePercentage: Float, bilinearFiltering: Boolean): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, bilinearFiltering)
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