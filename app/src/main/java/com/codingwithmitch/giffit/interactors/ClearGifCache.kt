package com.codingwithmitch.giffit.interactors

import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ClearGifCache {
    fun execute(): Flow<DataState<Unit>>
}

/**
 * Interactor for clearing all the cached files from the path provided via [CacheProvider].
 */
class ClearGifCacheInteractor
constructor(
    private val cacheProvider: CacheProvider
): ClearGifCache {
    override fun execute(): Flow<DataState<Unit>> = flow {
        emit(Loading(Active()))
        try {
            clearGifCache(cacheProvider)
            emit(Data(Unit)) // Done
        } catch (e: Exception) {
            emit(Error(e.message ?: CLEAR_CACHED_FILES_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val CLEAR_CACHED_FILES_ERROR = "An error occurred deleting the cached files."

        /**
         * Clears all the cached files from the path provided via [CacheProvider].
         */
        private fun clearGifCache(
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
