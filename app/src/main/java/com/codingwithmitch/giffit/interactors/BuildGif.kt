package com.codingwithmitch.giffit.interactors

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.codingwithmitch.giffit.AnimatedGIFWriter
import com.codingwithmitch.giffit.BitmapUtils.checkFilePermissions
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

class BuildGif {

    fun execute(
        context: Context,
        bitmaps: List<Bitmap>,
        onSaved: (Uri) -> Unit,
        launchPermissionRequest: () -> Unit
    ): Flow<DataState<Nothing>> =  flow {
        emit(Loading(LOADING))
        try {
            buildGifFromBitmapsAndSave(
                context = context,
                bitmaps = bitmaps,
                onSaved = onSaved,
                launchPermissionRequest = launchPermissionRequest
            )
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(IDLE))
    }

    private fun buildGifFromBitmapsAndSave(
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
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun Context.saveGifToScopedStorage(
        bytes: ByteArray,
        onSaved: (Uri) -> Unit
    ) {
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
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
    }
}









