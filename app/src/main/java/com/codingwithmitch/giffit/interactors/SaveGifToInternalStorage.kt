package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.codingwithmitch.giffit.FileNameBuilder
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * TODO("ktdoc")
 * Does not need permissions to write/read to internal storage.
 */
class SaveGifToInternalStorage
constructor(
    private val cacheProvider: CacheProvider
){

    fun execute(
        contentResolver: ContentResolver,
        bytes: ByteArray,
    ): Flow<DataState<Uri>> = flow {
        emit(Loading(Active()))
        try {
            emitAll(
                saveGifToInternalStorage(
                    contentResolver = contentResolver,
                    bytes = bytes,
                    fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        "${FileNameBuilder.buildFileNameAPI26()}.gif"
                    } else {
                        "${FileNameBuilder.buildFileName()}.gif"
                    }
                )
            )
        } catch (e: Exception) {
            Log.e(Constants.TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: SAVE_GIF_TO_INTERNAL_STORAGE_ERROR))
        }
        emit(Loading(Idle))
    }

    private fun saveGifToInternalStorage(
        contentResolver: ContentResolver,
        bytes: ByteArray,
        fileName: String,
    ): Flow<DataState<Uri>> = flow {
        val file = File.createTempFile(fileName, null, cacheProvider.gifCache())
        val uri = file.toUri()
        contentResolver.openOutputStream(uri)?.let { os ->
            os.write(bytes)
            os.flush()
            os.close()
            emit(Data(uri))
        } // <-- Don't need to throw since openOutputStream will.
    }

    companion object {
        const val SAVE_GIF_TO_INTERNAL_STORAGE_ERROR = "An error occurred while trying to save the" +
                " gif to internal storage."
    }

}