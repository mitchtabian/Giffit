package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Gets the size (in bytes) of the asset from Uri.
 */
class GetAssetSize {

    fun execute(
        contentResolver: ContentResolver,
        uri: Uri?,
    ): Flow<DataState<Int>> = flow {
        emit(Loading(Active()))
        try {
            check(uri != null) { "Null asset Uri." }
            // Ignore warning here since we are surrounding in try/catch.
            // If the warning bothers you, use runCatching {...}
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                emit(Data(inputStream.available()))
                inputStream.close()
            } else {
                throw Exception(GET_ASSET_SIZE_ERROR)
            }
        } catch (e: Exception) {
            emit(Error(e.message ?: GET_ASSET_SIZE_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val GET_ASSET_SIZE_ERROR =
            "Something went wrong getting the size of the asset."
    }
}