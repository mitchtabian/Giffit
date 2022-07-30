package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toFile
import com.codingwithmitch.giffit.domain.*
import com.codingwithmitch.giffit.interactors.BuildGif
import com.codingwithmitch.giffit.interactors.BuildGif.Companion.NO_BITMAPS_ERROR
import com.codingwithmitch.giffit.interactors.GetAssetSize
import com.codingwithmitch.giffit.interactors.SaveGifToInternalStorage
import com.codingwithmitch.giffit.util.buildBitmap
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BuildGifTest {

    private lateinit var buildGif: BuildGif

    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())
    private val versionProvider = RealVersionProvider()
    private val saveGifToInternalStorage = SaveGifToInternalStorage(cacheProvider, versionProvider)

    // Build some dummy bitmaps to build a gif with
    private val bitmaps: List<Bitmap> by lazy {
        val bmps: MutableList<Bitmap> = mutableListOf()
        repeat(5) {
            bmps.add(buildBitmap(RuntimeEnvironment.getApplication().resources))
        }
        bmps.toList()
    }

    @Before
    fun init() {
        buildGif = BuildGif(saveGifToInternalStorage)
    }

    @Test
    fun `verify gif is built from bitmaps`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        val emissions = buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = bitmaps
        ).toList()

        // Confirm the gif is saved to the cache directory
        val expectedFilePath = cacheProvider.gifCache().path
        val returnedUri = (emissions[2] as DataState.Data<Uri>).data
        val actualFilePath = returnedUri?.toFile()?.path
        assertThat(actualFilePath, containsString(expectedFilePath))
        assertThat(cacheProvider.gifCache().listFiles().size, equalTo(1))

        // Confirm the other emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[3], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun `verify error is thrown if bitmap list is empty`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        val emissions = buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = listOf() // empty list of bitmaps
        ).toList()

        // Confirm the other emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>(NO_BITMAPS_ERROR)))
        assertThat(emissions[2], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun `verify error is thrown if cannot save to cache`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Force an exception
        val fakeVersionProvider: VersionProvider = object: VersionProvider {
            override fun provideVersion(): Int {
                throw Exception("Can't save to cache or something who knows.")
            }
        }
        val saveGifToInternalStorage = SaveGifToInternalStorage(
            cacheProvider = cacheProvider,
            versionProvider = fakeVersionProvider
        )

        val buildGif = BuildGif(
            saveGifToInternalStorage = saveGifToInternalStorage
        )
        val emissions = buildGif.execute(
            contentResolver = contentResolver,
            bitmaps = bitmaps
        ).toList()

        // Confirm the other emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[2], equalTo(DataState.Error<Uri>("Can't save to cache or something who knows.")))
        assertThat(emissions[3], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))
    }
}











