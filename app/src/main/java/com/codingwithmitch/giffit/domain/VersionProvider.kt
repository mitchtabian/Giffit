package com.codingwithmitch.giffit.domain

import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the [Build.VERSION.SDK_INT] in app code. This makes checks on the API
 *  simple for Unit tests.
 */
interface VersionProvider {
    fun provideVersion(): Int
}

@Singleton
class RealVersionProvider
@Inject
constructor(): VersionProvider {
    override fun provideVersion() = Build.VERSION.SDK_INT
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VersionProviderModule {
    @Binds
    abstract fun provideVersionProvider(versionProvider: RealVersionProvider): VersionProvider
}