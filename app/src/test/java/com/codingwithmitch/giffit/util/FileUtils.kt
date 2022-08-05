package com.codingwithmitch.giffit.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.codingwithmitch.giffit.R
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