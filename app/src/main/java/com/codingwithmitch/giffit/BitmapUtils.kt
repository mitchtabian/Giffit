package com.codingwithmitch.giffit

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.IOException

object BitmapUtils {

    fun buildGifFromBitmapsAndSave(
        context: Context,
        bitmaps: List<Bitmap>,
        onSaved: (Uri) -> Unit,
        launchPermissionRequest: () -> Unit
    ) {
        val writer = AnimatedGIFWriter(true)
        val bos = ByteArrayOutputStream()
        writer.prepareForWrite(bos, -1, -1)
        for(bitmap in bitmaps) {
            writer.writeFrame(bos, bitmap)
        }
        writer.finishWrite(bos)
        val byteArray = bos.toByteArray()
        context.saveGif(
            byteArray,
            onSaved = onSaved,
            launchPermissionRequest = launchPermissionRequest
        )
    }

    private fun Context.saveGif(
        bytes: ByteArray,
        onSaved: (Uri) -> Unit,
        launchPermissionRequest: () -> Unit,
    ) {
        // If API >= 29 we can use scoped storage and don't require permission to save images.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveGifToScopedStorage(bytes) {
                onSaved(it)
            }
        } else {
            // Scoped storage doesn't exist before Android 29 so need to check permissions
            if (checkFilePermissions()) {
                saveGifToStorage(bytes) {
                    onSaved(it)
                }
            } else {
                launchPermissionRequest()
            }
        }
    }

    private fun Context.saveGifToStorage(
        bytes: ByteArray,
        onSaved: (Uri) -> Unit
    ) {
        try {
            // Add content values so media is discoverable by android and added to common directories.
            val contentValues = ContentValues()
            val fileName = FileNameBuilder.buildFileName()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.gif")
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.let { os ->
                    os.write(bytes)
                    os.flush()
                    os.close()
                    onSaved(uri)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun Context.saveGifToScopedStorage(
        bytes: ByteArray,
        onSaved: (Uri) -> Unit
    ) {
        try {
            val externalUri: Uri =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            // Add content values so media is discoverable by android and added to common directories.
            val contentValues = ContentValues()
            val fileName = FileNameBuilder.buildFileNameAPI26()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}.gif")
            contentResolver.insert(externalUri, contentValues)?.let { fileUri ->
                contentResolver.openOutputStream(fileUri)?.let { os ->
                    os.write(bytes)
                    os.flush()
                    os.close()
                    onSaved(fileUri)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

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