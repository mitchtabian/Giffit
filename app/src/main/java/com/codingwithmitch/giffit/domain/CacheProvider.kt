package com.codingwithmitch.giffit.domain

import android.content.Context
import java.io.File

/**
 * TODO("ktdoc")
 */
interface CacheProvider {
    fun gifCache(): File
}

/**
 * TODO("ktdoc")
 */
class RealCacheProvider
constructor(
    private val context: Context
): CacheProvider {

    override fun gifCache(): File {
        val file = File("${context.cacheDir.path}/temp_gifs")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

}