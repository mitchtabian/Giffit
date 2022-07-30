package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.AnimatedGIFWriter
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

/**
 * Builds a gif given a list of [Bitmap]'s and saves it to internal storage.
 * We do not need read/write permission because saving to the cache does
 *  not require it.
 */
class BuildGif(
    private val saveGifToInternalStorage: SaveGifToInternalStorage
) {

    fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<Uri>> =  flow {
        emit(Loading(Active()))
        try {
            check(bitmaps.isNotEmpty()) { NO_BITMAPS_ERROR }
            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for(bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }
            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            emitAll(
                saveGifToInternalStorage.execute(
                    contentResolver = contentResolver,
                    bytes = byteArray,
                )
            )
        } catch (e: Exception) {
            Log.e(Constants.TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
        const val NO_BITMAPS_ERROR = "\"You can't build a gif when there are no Bitmaps!\""
    }
}









