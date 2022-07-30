package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
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

/**
 * Saves a cached [Uri] to external storage. A cached [Uri] is defined as a [Uri] that is
 *  saved to [CacheProvider.gifCache()].
 *
 * There is two possible pathways:
 * (1) On API 29 + you do not need permissions to write/read to internal storage (Scoped Storage).
 *   This pathway is via [saveGifToScopedStorage].
 *  (2) On API 28- you must ask for read/write permissions.
 *   This pathway is via [saveGifToStorage].
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
        when {
            // If API >= 29 we can use scoped storage and don't require permission to save images.
            versionProvider.provideVersion() >= Build.VERSION_CODES.Q -> emitAll(
                saveGifToScopedStorage(
                    contentResolver = contentResolver,
                    cachedUri = cachedUri
                )
            )
            // Scoped storage doesn't exist before Android 29 so need to check permissions
            checkFilePermissions() -> emitAll(
                saveGifToStorage(
                    contentResolver = contentResolver,
                    cachedUri = cachedUri
                )
            )
            // If we made it this far, read/write permission has not been accepted.
            else -> launchPermissionRequest()
        }
        emit(Loading(Idle))
    }

    private fun getBytesFromUri(
        contentResolver: ContentResolver,
        uri: Uri
    ): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: ByteArray(0)
        inputStream?.close()
        return bytes
    }

    private fun saveGifToStorage(
        contentResolver: ContentResolver,
        cachedUri: Uri,
    ): Flow<DataState<Uri>> = flow {
        try {
            // Get bytes from cached file
            val bytes = getBytesFromUri(
                contentResolver = contentResolver,
                uri = cachedUri
            )
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
        cachedUri: Uri,
    ): Flow<DataState<Uri>> = flow {
        try {
            // Get bytes from cached file
            val bytes = getBytesFromUri(
                contentResolver = contentResolver,
                uri = cachedUri
            )
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