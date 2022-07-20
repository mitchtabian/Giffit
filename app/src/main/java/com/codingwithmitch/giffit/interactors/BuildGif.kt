package com.codingwithmitch.giffit.interactors

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.AnimatedGIFWriter
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.*
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream

/**
 * TODO("ktdoc")
 */
class BuildGif(
    private val saveGifToStorage: SaveGifToStorage
) {

    fun execute(
        contentResolver: ContentResolver,
        bitmaps: List<Bitmap>,
        launchPermissionRequest: () -> Unit,
        checkFilePermissions: () -> Boolean,
    ): Flow<DataState<Uri>> =  flow {
        emit(Loading(Active()))
        try {
            val writer = AnimatedGIFWriter(true)
            val bos = ByteArrayOutputStream()
            writer.prepareForWrite(bos, -1, -1)
            for(bitmap in bitmaps) {
                writer.writeFrame(bos, bitmap)
            }
            writer.finishWrite(bos)
            val byteArray = bos.toByteArray()
            emitAll(
                saveGifToStorage.execute(
                    contentResolver = contentResolver,
                    bytes = byteArray,
                    launchPermissionRequest = launchPermissionRequest,
                    checkFilePermissions = checkFilePermissions
                )
            )
        } catch (e: Exception) {
            emit(Error(e.message ?: BUILD_GIF_ERROR))
        }
        emit(Loading(Idle))
    }

    companion object {
        const val BUILD_GIF_ERROR = "An error occurred while building the gif."
    }
}









