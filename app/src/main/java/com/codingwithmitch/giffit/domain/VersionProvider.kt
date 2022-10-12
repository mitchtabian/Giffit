package com.codingwithmitch.giffit.domain

import android.os.Build

/**
 * Provides the [Build.VERSION.SDK_INT] in app code. This makes checks on the API
 *  simple for Unit tests.
 */
interface VersionProvider {
    fun provideVersion(): Int
}

class RealVersionProvider
constructor(): VersionProvider {
    override fun provideVersion() = Build.VERSION.SDK_INT
}