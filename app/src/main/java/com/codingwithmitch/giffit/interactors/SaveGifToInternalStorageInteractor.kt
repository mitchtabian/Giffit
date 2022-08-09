package com.codingwithmitch.giffit.interactors

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.*
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Interactor for saving a gif (from [ByteArray]) to internal storage.
 */
class SaveGifToInternalStorageInteractor
@Inject
constructor(
    private val cacheProvider: CacheProvider,
    private val versionProvider: VersionProvider
){

    fun execute(
        contentResolver: ContentResolver,
        bytes: ByteArray,
    ): Flow<DataState<Uri>> = flow {
        emit(Loading(Active()))
        try {
            val uri = saveGifToInternalStorage(
                contentResolver = contentResolver,
                bytes = bytes,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider
            )
            emit(Data(uri))
        } catch (e: Exception) {
            Log.e(TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: SAVE_GIF_TO_INTERNAL_STORAGE_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "An error occurred while trying to save the" +
                " gif to internal storage."

        /**
         * Save a [ByteArray] to internal storage.
         * You do not need permissions to write/read to internal storage at any API level.
         *
         * Suppresses the version warning since we're using [VersionProvider].
         */
        @SuppressLint("NewApi")
        fun saveGifToInternalStorage(
            contentResolver: ContentResolver,
            bytes: ByteArray,
            cacheProvider: CacheProvider,
            versionProvider: VersionProvider,
        ): Uri {
            val fileName = if (versionProvider.provideVersion() >= Build.VERSION_CODES.O) {
                "${FileNameBuilder.buildFileNameAPI26()}.gif"
            } else {
                "${FileNameBuilder.buildFileName()}.gif"
            }
            val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
            val uri = file.toUri()
            return contentResolver.openOutputStream(uri)?.let { os ->
                os.write(bytes)
                os.flush()
                os.close()
                uri
            } ?: throw Exception(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)
        }
    }
}