package com.codingwithmitch.giffit.interactors.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.FileNameBuilder
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.domain.util.AnimatedGIFWriter
import com.codingwithmitch.giffit.interactors.BuildGifInteractor
import com.codingwithmitch.giffit.interactors.ResizeGif
import com.codingwithmitch.giffit.interactors.ResizeGif.*
import java.io.ByteArrayOutputStream
import java.io.File

object GifUtil {

    /**
     * Build a Gif from a list of [Bitmap]'s and save to internal storage in [CacheProvider.gifCache].
     * Return a [Pair] containing the [Uri] and the Size of the new [Bitmap].
     */
    fun buildGifAndSaveToInternalStorage(
        contentResolver: ContentResolver,
        versionProvider: VersionProvider,
        cacheProvider: CacheProvider,
        bitmaps: List<Bitmap>
    ): ResizeGifResult {
        check(bitmaps.isNotEmpty()) { BuildGifInteractor.NO_BITMAPS_ERROR }
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
        return ResizeGifResult(uri, byteArray.size)
    }

    /**
     * Save a [ByteArray] to internal storage.
     * You do not need permissions to write/read to internal storage at any API level.
     *
     * Suppresses the version warning since we're using [VersionProvider].
     */
    @SuppressLint("NewApi")
    private fun saveGifToInternalStorage(
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

    const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "An error occurred while trying to save the" +
            " gif to internal storage."
}