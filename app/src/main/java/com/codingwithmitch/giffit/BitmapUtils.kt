package com.codingwithmitch.giffit

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

object BitmapUtils {

    fun Context.checkFilePermissions(): Boolean  {
        val writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    fun Context.discardGif(uri: Uri) {
        contentResolver.delete(uri, null, null)
    }

    /**
     * Estimates the size of the cropped image by referencing the original uncropped
     *  image and multiplying by the area difference.
     *  @param originalWidth: Width of uncropped image.
     *  @param originalHeight: Height of uncropped image.
     *  @param croppedWidth: Width of cropped image.
     *  @param croppedHeight: Height of cropped image.
     *  @param uncroppedImageSize: Size (in bytes) of original uncropped image.
     */
    fun estimateCroppedImageSize(
        originalWidth: Int,
        originalHeight: Int,
        croppedWidth: Int,
        croppedHeight: Int,
        uncroppedImageSize: Int,
    ): Int {
        // We can estimate the size of the cropped image using the difference in area.
        val area = originalHeight * originalWidth
        val croppedArea = croppedHeight * croppedWidth
        val deltaArea = area - croppedArea
        // How much % of the area was removed from the crop
        val pctAreaDelta = deltaArea.toFloat() / area.toFloat()
        return (uncroppedImageSize.toFloat() * (1 - pctAreaDelta)).toInt()
    }
}