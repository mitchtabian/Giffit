package com.codingwithmitch.giffit.interactors

import android.util.Log
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.Constants.TAG
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Interactor for clearing all the cached files from the path provided via [CacheProvider].
 */
class ClearGifCacheInteractor
constructor(
    private val cacheProvider: CacheProvider
){

    fun execute(): Flow<DataState<Unit>> = flow {
        emit(Loading(Active()))
        try {
            clearGifCache(cacheProvider)
            emit(Data(Unit)) // Done
        } catch (e: Exception) {
            Log.e(TAG, "ClearCachedFiles: ", e)
            emit(Error(e.message ?: CLEAR_CACHED_FILES_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val CLEAR_CACHED_FILES_ERROR = "An error occurred deleting the cached files."

        /**
         * Clears all the cached files from the path provided via [CacheProvider].
         */
        fun clearGifCache(
            cacheProvider: CacheProvider
        ) {
            val internalStorageDirectory = cacheProvider.gifCache()
            val files = internalStorageDirectory.listFiles()
            for(file in files) {
                file.delete()
            }
        }
    }
}
















