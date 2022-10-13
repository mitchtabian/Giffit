package com.codingwithmitch.giffit.domain

import android.app.Application
import java.io.File

interface CacheProvider {

    /**
     * Provides the directory where cached gif files are kept.
     */
    fun gifCache(): File
}

class RealCacheProvider
constructor(
    private val app: Application
): CacheProvider {

    override fun gifCache(): File {
        val file = File("${app.cacheDir.path}/temp_gifs")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }
}