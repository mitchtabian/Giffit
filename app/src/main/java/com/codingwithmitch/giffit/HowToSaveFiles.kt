package com.codingwithmitch.giffit

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getContentUri
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.Q)
private fun Activity.saveFileToScopedStorage(displayName: String, bitmap: Bitmap) {
    val externalUri: Uri = getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    // Add content values so media is discoverable by android and added to common directories.
    val contentValues = ContentValues()
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
    contentResolver.insert(externalUri, contentValues)?.let { fileUri ->
        try {
            val outputStream: OutputStream? = contentResolver.openOutputStream(fileUri)
            outputStream.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                out?.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

private fun Activity.saveFileToStorage(displayName: String, bmp: Bitmap) {
    val file = File("${Environment.getExternalStorageDirectory()}/Pictures", "$displayName.png")
    if (!file.exists()) {
        try {
            // Add content values so media is discoverable by android and added to common directories.
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.let { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 85, fos)
                    fos.flush()
                    fos.close()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}