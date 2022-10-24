package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.domain.CacheProvider
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.interactors.BuildGif.*
import com.codingwithmitch.giffit.interactors.util.GifUtil
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BuildGif {

    fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>>

    data class BuildGifResult(
        val uri: Uri,
        val gifSize: Int,
    )
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class BuildGifModule {
    @Binds
    abstract fun provideBuildGif(buildGif: BuildGifInteractor): BuildGif
}

/**
 * Interactor for building a gif given a list of [Bitmap]'s. The resulting gif is saved it to internal storage.
 * We do not need read/write permission because saving to the cache does not require it.
 */
class BuildGifInteractor
@Inject
constructor(
    private val cacheProvider: CacheProvider,
    private val versionProvider: VersionProvider
): BuildGif {

    override fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
    ): Flow<DataState<BuildGifResult>> =  flow {
        emit(Loading(Active()))
        try {
            val uriSizePair = GifUtil.buildGifAndSaveToInternalStorage(
                contentResolver = contentResolver,
                versionProvider = versionProvider,
                cacheProvider = cacheProvider,
                bitmaps = bitmaps
            )
            emit(Data(BuildGifResult(uriSizePair.uri, uriSizePair.gifSize)))
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
        const val NO_BITMAPS_ERROR = "You can't build a gif when there are no Bitmaps!"
    }
}
