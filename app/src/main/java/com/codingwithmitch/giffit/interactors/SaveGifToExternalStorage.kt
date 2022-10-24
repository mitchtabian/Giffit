package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.FileNameBuilder
import com.codingwithmitch.giffit.domain.VersionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

interface SaveGifToExternalStorage {

    fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri: Uri,
        checkFilePermissions: () -> Boolean,
    ): Flow<DataState<Unit>>
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class SaveGifToExternalStorageModule {
    @Binds
    abstract fun provideSaveGifToExternalStorage(
        saveGifToExternalStorage: SaveGifToExternalStorageInteractor
    ): SaveGifToExternalStorage
}

/**
 * Interactor for saving a cached [Uri] to external storage. A cached [Uri] is defined as a [Uri] that is
 *  saved to [CacheProvider.gifCache()].
 *
 * There is two possible pathways:
 * (1) On API 29 + you do not need permissions to write/read to internal storage (Scoped Storage).
 *   This pathway is via [saveGifToScopedStorage].
 *  (2) On API 28- you must ask for read/write permissions.
 *   This pathway is via [saveGifToExternalStorage].
 */
class SaveGifToExternalStorageInteractor
@Inject
constructor(
    private val versionProvider: VersionProvider,
): SaveGifToExternalStorage {

    @SuppressLint("NewApi")
    override fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri: Uri,
        checkFilePermissions: () -> Boolean,
    ): Flow<DataState<Unit>> = flow {
        try {
            emit(Loading(Active()))
            when {
                // If API >= 29 we can use scoped storage and don't require permission to save images.
                versionProvider.provideVersion() >= Build.VERSION_CODES.Q -> {
                    saveGifToScopedStorage(
                        contentResolver = contentResolver,
                        cachedUri = cachedUri
                    )
                    emit(Data(Unit))
                }
                // Scoped storage doesn't exist before Android 29 so need to check permissions
                checkFilePermissions() -> {
                    saveGifToExternalStorage(
                        contentResolver = contentResolver,
                        context = context,
                        cachedUri = cachedUri
                    )
                    emit(Data(Unit))
                }
                // If we made it this far, read/write permission has not been accepted.
                else -> emit(Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
            }
        } catch (e: Exception) {
            emit(Error(e.message ?: SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR =
            "An error occurred while trying to save the " +
                    "gif to external storage."

        private fun getBytesFromUri(
            contentResolver: ContentResolver,
            uri: Uri
        ): ByteArray {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: ByteArray(0)
            inputStream?.close()
            return bytes
        }

        /**
         * Save a gif [Uri] to external storage without scoped storage.
         */
        fun saveGifToExternalStorage(
            contentResolver: ContentResolver,
            context: Context,
            cachedUri: Uri,
        ) {
            // Get bytes from cached file
            val bytes = getBytesFromUri(
                contentResolver = contentResolver,
                uri = cachedUri
            )
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val fileName = FileNameBuilder.buildFileName()
            val fileSavePath = File(picturesDir, "$fileName.gif")

            // Make sure the Pictures directory exists.
            picturesDir.mkdirs()

            val fos = FileOutputStream(fileSavePath)
            fos.write(bytes)
            fos.close()

            // Tell the media scanner about the new file so that it is
            // immediately available to the user. Otherwise you'll have to restart the device to see it
            MediaScannerConnection.scanFile(
                context,
                arrayOf(fileSavePath.toString()),
                null
            ) { _, _ ->
            }
        }
    }

    /**
     * Save a gif [Uri] to scoped storage.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveGifToScopedStorage(
        contentResolver: ContentResolver,
        cachedUri: Uri,
    ) {
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
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        val uri = contentResolver.insert(externalUri, contentValues) ?: throw Exception("Error inserting the uri into storage.")
        return contentResolver.openOutputStream(uri)?.let { os ->
            os.write(bytes)
            os.flush()
            os.close()
        } ?: throw Exception(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR)
    }
}