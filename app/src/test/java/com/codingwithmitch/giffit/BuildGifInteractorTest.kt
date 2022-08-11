package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toFile
import com.codingwithmitch.giffit.domain.*
import com.codingwithmitch.giffit.interactors.BuildGif
import com.codingwithmitch.giffit.interactors.BuildGif.*
import com.codingwithmitch.giffit.interactors.BuildGifInteractor
import com.codingwithmitch.giffit.interactors.BuildGifInteractor.Companion.NO_BITMAPS_ERROR
import com.codingwithmitch.giffit.interactors.BuildGifInteractor.Companion.saveGifToInternalStorage
import com.codingwithmitch.giffit.util.buildBitmap
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BuildGifInteractorTest {

    private lateinit var buildGifInteractor: BuildGifInteractor

    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())
    private val versionProvider = RealVersionProvider()

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
        buildGifInteractor = BuildGifInteractor(
            cacheProvider = cacheProvider,
            versionProvider = versionProvider
        )
    }

    @Test
    fun `verify gif is built from bitmaps`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        val emissions = buildGifInteractor.execute(
            contentResolver = contentResolver,
            bitmaps = bitmaps
        ).toList()

        // Confirm the gif is saved to the cache directory
        val expectedFilePath = cacheProvider.gifCache().path
        val returnedUri = (emissions[1] as DataState.Data<BuildGifResult>).data?.uri
        val actualFilePath = returnedUri?.toFile()?.path
        assertThat(actualFilePath, containsString(expectedFilePath))
        assertThat(cacheProvider.gifCache().listFiles().size, equalTo(1))

        // Confirm the other emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[2], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun `verify error is thrown if bitmap list is empty`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        val emissions = buildGifInteractor.execute(
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

        val buildGifInteractor = BuildGifInteractor(
            cacheProvider = cacheProvider,
            versionProvider = fakeVersionProvider
        )
        val emissions = buildGifInteractor.execute(
            contentResolver = contentResolver,
            bitmaps = bitmaps
        ).toList()

        // Confirm the other emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>("Can't save to cache or something who knows.")))
        assertThat(emissions[2], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyBytesAreSavedToInternalStorage() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Execute the use-case
        val uri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = versionProvider
        )
        val file = uri.toFile()

        // Confirm the file exists in the cache
        assertThat(cacheProvider.gifCache().listFiles().size, equalTo(1))
        assertThat(cacheProvider.gifCache().listFiles()[0].name, equalTo(file.name))
    }
}











