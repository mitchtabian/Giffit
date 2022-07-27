package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileInputStream

/**
 * Saves a [Uri] to external storage.
 * On [Build.VERSION_CODES.Q] and above we can use scoped storage and do not need to ask for
 *  permission.
 * On [Build.VERSION_CODES.P] and below we need to ask for the users permission to read/write
 *  to external storage.
 */
class SaveGifToExternalStorage(
    private val versionProvider: VersionProvider,
) {

    /**
     * Suppress the SDK_INT error since we're using [VersionProvider].
     */
    @SuppressLint("NewApi")
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
            if (versionProvider.provideVersion() >= Build.VERSION_CODES.Q) {
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
                    emit(Data(uri))
                } ?: emit(Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
            } ?: emit(Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        } catch (e: Exception) {
            emit(Error(e.message ?: SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveGifToScopedStorage(
        contentResolver: ContentResolver,
        bytes: ByteArray,
    ): Flow<DataState<Uri>> = flow {
        try {
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
            } ?: emit(Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        } catch (e: Exception) {
            Log.e(TAG, "saveGifToScopedStorage: ", e)
            emit(Error(e.message ?: SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }
    }

    companion object {
        const val SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR = "An error occurred while trying to save the " +
                "gif to external storage."
    }

}