package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.domain.Constants
import com.codingwithmitch.giffit.domain.DataState
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
        emit(DataState.Loading(LOADING))
        try {
            check(uri != null) { "Null asset Uri." }
            // Ignore warning here since we are surrounding in try/catch.
            // If the warning bothers you, use runCatching {...}
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                emit(DataState.Data(inputStream.available()))
                inputStream.close()
            } else {
                throw Exception(GET_ASSET_SIZE_ERROR)
            }
        } catch (e: Exception) {
            Log.d(Constants.TAG, "Exception: ${e}")
            emit(DataState.Error(e.message ?: GET_ASSET_SIZE_ERROR))
        }
        // TODO("Loading is probably pointless here since crop will launch at same time")
        emit(DataState.Loading(IDLE))
    }

    companion object {
        const val GET_ASSET_SIZE_ERROR =
            "Something went wrong getting the size of the asset."
    }
}