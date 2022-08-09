package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.AnimatedGIFWriter
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Interactor for building a gif given a list of [Bitmap]'s. The resulting gif is saved it to internal storage.
 * We do not need read/write permission because saving to the cache does not require it.
 */
class BuildGifInteractor
@Inject
constructor(
    private val cacheProvider: CacheProvider,
    private val versionProvider: VersionProvider
) {

    data class BuildGifResult(
        val uri: Uri,
        val gifSize: Int,
    )

    fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>> =  flow {
        emit(Loading(Active()))
        try {
            val result = buildGifAndSaveToInternalStorage(
                contentResolver = contentResolver,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider,
                bitmaps = bitmaps
            )
            emit(Data(result))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
        const val NO_BITMAPS_ERROR = "You can't build a gif when there are no Bitmaps!"

        /**
         * Build a Gif from a list of [Bitmap]'s and save to internal storage in [CacheProvider.gifCache].
         * Return a [BuildGifResult] containing the [Uri] and the Size of the new [Bitmap].
         */
        fun buildGifAndSaveToInternalStorage(
            contentResolver: ContentResolver,
            versionProvider: VersionProvider,
            cacheProvider: CacheProvider,
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
            val uri = SaveGifToInternalStorageInteractor.saveGifToInternalStorage(
                contentResolver = contentResolver,
                bytes = byteArray,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider
            )
            return BuildGifResult(uri, byteArray.size)
        }
    }
}









