package com.codingwithmitch.giffit

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.content.ContextCompat

object BitmapUtils {

    fun Context.checkFilePermissions(): Boolean  {
        val writePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * @param bilinearFiltering: For some reason bilinear-filtering does some weird stuff in unit tests.
     *  It actually increases the size of the original bitmap. So I added it as a param so we can disable
     *  for unit tests.
     */
    fun resizeBitmap(bitmap: Bitmap, sizePercentage: Float, bilinearFiltering: Boolean): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, bilinearFiltering)
    }
}