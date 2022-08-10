package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.codingwithmitch.giffit.AnimatedGIFWriter
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.io.File
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
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "An error occurred while trying to save the" +
                " gif to internal storage."

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
            val uri = saveGifToInternalStorage(
                contentResolver = contentResolver,
                bytes = byteArray,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider
            )
            return BuildGifResult(uri, byteArray.size)
        }

        /**
         * Save a [ByteArray] to internal storage.
         * You do not need permissions to write/read to internal storage at any API level.
         *
         * Suppresses the version warning since we're using [VersionProvider].
         */
        @SuppressLint("NewApi")
        @VisibleForTesting fun saveGifToInternalStorage(
            contentResolver: ContentResolver,
            bytes: ByteArray,
            cacheProvider: CacheProvider,
            versionProvider: VersionProvider,
        ): Uri {
            val fileName = if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                "${FileNameBuilder.buildFileNameAPI26()}.gif"
            } else {
                "${FileNameBuilder.buildFileName()}.gif"
            }
            val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
            val uri = file.toUri()
            return contentResolver.openOutputStream(uri)?.let { os ->
                os.write(bytes)
                os.flush()
                os.close()
                uri
            } ?: throw Exception(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
        }
    }
}









