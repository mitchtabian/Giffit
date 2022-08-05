package com.codingwithmitch.giffit

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.GetAssetSizeInteractor
import com.codingwithmitch.giffit.interactors.GetAssetSizeInteractor.Companion.GET_ASSET_SIZE_ERROR
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import com.codingwithmitch.giffit.util.saveBytesToInternalStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetAssetSizeInteractorTest {

    private lateinit var getAssetSizeInteractor: GetAssetSizeInteractor

    @Before
    fun init() {
        getAssetSizeInteractor = GetAssetSizeInteractor()
    }

    @Test
    fun verifyEmissions() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)
        val bitmapSize = byteArray.size

        // Save to cache.
        // Note: Technically it will save as a gif but it doesn't matter for the test.
        val cacheProvider = RealCacheProvider(context)
        val uri = saveBytesToInternalStorage(
            cacheProvider = cacheProvider,
            versionProvider = RealVersionProvider(),
            contentResolver = contentResolver,
            bytes = byteArray
        )
        val file = uri.toFile()

        // Confirm the file exists in the cache
        assertThat(cacheProvider.gifCache().listFiles().size, equalTo(1))
        assertThat(cacheProvider.gifCache().listFiles()[0].name, equalTo(file.name))

        // Configure ContentResolver. This is required in unit tests with Roboelectric.
        // Otherwise the inputstream will not write anything.
        val shadowContentResolver = shadowOf(context.contentResolver)
        shadowContentResolver.registerInputStream(uri, ByteArrayInputStream(ByteArray(bitmapSize)))

        // Execute the use-case
        val emissions = getAssetSizeInteractor.execute(
            contentResolver = contentResolver,
            uri = uri
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Data<Int>(bitmapSize)))
        assertThat(emissions[2], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyNullUriThrows() = runTest {
        // Execute the use-case
        val emissions = getAssetSizeInteractor.execute(
            contentResolver = RuntimeEnvironment.getApplication().contentResolver,
            uri = null
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>("Null asset Uri.")))
        assertThat(emissions[2], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyInputStreamErrorThrows() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val cacheDir = context.cacheDir

        // Create a dummy file
        val file = File.createTempFile("someBitmap.png", null, cacheDir)

        // Execute the use-case
        val emissions = getAssetSizeInteractor.execute(
            contentResolver = mock {
                // Force throw exception
                on { openInputStream(any()) } doThrow FileNotFoundException("Something is busted")
            },
            uri = file.toUri()
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>("Something is busted")))
        assertThat(emissions[2], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyErrorEmissionIfNullInputStream() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val cacheDir = context.cacheDir

        // Create a dummy file
        val file = File.createTempFile("someBitmap.png", null, cacheDir)

        // Execute the use-case
        val emissions = getAssetSizeInteractor.execute(
            contentResolver = mock {
                // Force throw exception
                on { openInputStream(any()) } doReturn null
            },
            uri = file.toUri()
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>(GET_ASSET_SIZE_ERROR)))
        assertThat(emissions[2], equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))
    }
}











