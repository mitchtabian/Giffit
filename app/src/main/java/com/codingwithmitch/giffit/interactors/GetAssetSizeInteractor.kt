package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// TODO("Delete this???")
/**
 * Interactor for getting the size (in bytes) from a [Uri].
 */
class GetAssetSizeInteractor {

    fun execute(
        contentResolver: ContentResolver,
        uri: Uri?,
    ): Flow<DataState<Int>> = flow {
        emit(Loading(Active()))
        try {
            val size = getAssetSize(
                contentResolver = contentResolver,
                uri = uri
            )
            emit(Data(size))
        } catch (e: Exception) {
            Log.e(TAG, "GetAssetSize: ", e)
            emit(Error(e.message ?: GET_ASSET_SIZE_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val GET_ASSET_SIZE_ERROR =
            "Something went wrong getting the size of the asset."

        /**
         * Gets the size (in bytes) of the asset from Uri.
         */
        fun getAssetSize(
            contentResolver: ContentResolver,
            uri: Uri?,
        ): Int {
            check(uri != null) { "Null asset Uri." }
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val size = inputStream.readBytes().size
                inputStream.close()
                return size
            } else {
                throw Exception(GET_ASSET_SIZE_ERROR)
            }
        }
    }
}