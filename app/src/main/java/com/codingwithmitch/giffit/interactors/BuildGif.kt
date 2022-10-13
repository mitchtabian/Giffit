package com.codingwithmitch.giffit.interactors

import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.domain.util.AnimatedGIFWriter
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.BuildGif.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

interface BuildGif {

    fun execute(
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>>

    data class BuildGifResult(
        val uri: Uri,
        val gifSize: Int,
    )
}

/**
 * Interactor for building a gif given a list of [Bitmap]'s. The resulting gif is saved it to internal storage.
 * We do not need read/write permission because saving to the cache does not require it.
 */
class BuildGifInteractor
constructor(): BuildGif {

    override fun execute(
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>> =  flow {
        emit(Loading(Active()))
        try {
            val result = buildGifAndSaveToInternalStorage(
                bitmaps = bitmaps
            )
            emit(Data(result))
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
        const val NO_BITMAPS_ERROR = "You can't build a gif when there are no Bitmaps!"
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "An error occurred while trying to save the" +
                " gif to internal storage."

        /**
         * Build a Gif from a list of [Bitmap]'s and save to internal storage in [CacheProvider.gifCache].
         * Return a [BuildGifResult] containing the [Uri] and the Size of the new [Bitmap].
         */
        fun buildGifAndSaveToInternalStorage(
            bitmaps: List<Bitmap>
        ): BuildGifResult {
            check(bitmaps.isNotEmpty()) { NO_BITMAPS_ERROR }
            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for(bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }
            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            val uri = saveGifToInternalStorage() // TODO
            return BuildGifResult(uri, byteArray.size)
        }

        fun saveGifToInternalStorage(): Uri {
            TODO("Save gif to cache")
        }
    }
}
