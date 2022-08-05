package com.codingwithmitch.giffit.util

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.codingwithmitch.giffit.R
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.interactors.SaveGifToInternalStorageInteractor
import kotlinx.coroutines.flow.toList
import java.io.ByteArrayOutputStream

/**
 * Creates a dummy [Bitmap].
 */
fun buildBitmap(resources: Resources): Bitmap {
    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.deal_with_it_sunglasses_default)
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    return bitmap
}

/**
 * Creates a dummy [Bitmap] and returns the [ByteArray] of the [Bitmap].
 */
fun buildBitmapByteArray(resources: Resources): ByteArray {
    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.deal_with_it_sunglasses_default)
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    return baos.toByteArray()
}

/**
 * Saves a [ByteArray] to internal storage and returns the [Uri] of the new file.
 */
suspend fun saveBytesToInternalStorage(
    cacheProvider: CacheProvider,
    versionProvider: VersionProvider,
    contentResolver: ContentResolver,
    bytes: ByteArray,
): Uri {
    val saveGifToInternalStorageInteractor = SaveGifToInternalStorageInteractor(cacheProvider, versionProvider)
    val storageEmissions = saveGifToInternalStorageInteractor.execute(
        contentResolver = contentResolver,
        bytes = bytes
    ).toList()
    return (storageEmissions[1] as DataState.Data).data ?: throw Exception("Uri cannot be null.")
}