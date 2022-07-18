package com.codingwithmitch.giffit

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
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

    fun resizeBitmap(bitmap: Bitmap, sizePercentage: Float): Bitmap {
        val targetWidth = (bitmap.width * sizePercentage).toInt()
        val targetHeight = (bitmap.height * sizePercentage).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}