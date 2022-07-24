package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileInputStream

/**
 * TODO("ktdoc")
 */
class SaveGifToExternalStorage {

    fun execute(
        contentResolver: ContentResolver,
        cachedUri: Uri,
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
    ): Flow<DataState<Uri>> = flow {
        emit(Loading(Active()))
        try {
            // Get bytes from cached file
            val bytes = getBytesFromUri(cachedUri)
            // If API >= 29 we can use scoped storage and don't require permission to save images.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                emitAll(
                    saveGifToScopedStorage(
                        contentResolver = contentResolver,
                        bytes = bytes
                    )
                )
            } else {
                // Scoped storage doesn't exist before Android 29 so need to check permissions
                if (checkFilePermissions()) {
                    emitAll(saveGifToStorage(contentResolver, bytes))
                } else {
                    launchPermissionRequest()
                }
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }
        emit(Loading(Idle))
    }

    private fun getBytesFromUri(uri: Uri): ByteArray {
        val file = File(uri.path)
        val fis = FileInputStream(file)
        val bytes = fis.readBytes()
        fis.close()
        return bytes
    }

    private fun saveGifToStorage(
        contentResolver: ContentResolver,
        bytes: ByteArray,
    ): Flow<DataState<Uri>> = flow {
        // Add content values so media is discoverable by android and added to common directories.
        val contentValues = ContentValues()
        val fileName = FileNameBuilder.buildFileName()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.gif")
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
            contentResolver.openOutputStream(uri)?.let { os ->
                os.write(bytes)
                os.flush()
                os.close()
                emit(Data(uri))
            } // <-- Don't need to throw since openOutputStream will.
        } ?: throw Exception("Error inserting the uri into storage.")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveGifToScopedStorage(
        contentResolver: ContentResolver,
        bytes: ByteArray,
    ): Flow<DataState<Uri>> = flow {
        val fileName = "${FileNameBuilder.buildFileNameAPI26()}.gif"
        val externalUri: Uri =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // Add content values so media is discoverable by android and added to common directories.
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        val uri = contentResolver.insert(externalUri, contentValues) ?: throw Exception("Error inserting the uri into storage.")
        contentResolver.openOutputStream(uri)?.let { os ->
            os.write(bytes)
            os.flush()
            os.close()
            emit(Data(uri))
        } // <-- Don't need to throw since openOutputStream will.
    }

    companion object {
        const val SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR = "An error occurred while trying to save the " +
                "gif to external storage."
    }

}