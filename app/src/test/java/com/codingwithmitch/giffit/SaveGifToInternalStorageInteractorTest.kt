package com.codingwithmitch.giffit

import android.net.Uri
import androidx.core.net.toFile
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.SaveGifToInternalStorageInteractor
import com.codingwithmitch.giffit.interactors.SaveGifToInternalStorageInteractor.Companion.SAVE_GIF_TO_INTERNAL_STORAGE_ERROR
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveGifToInternalStorageInteractorTest {

    private lateinit var saveGifToInternalStorageInteractor: SaveGifToInternalStorageInteractor
    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())

    @Before
    fun init() {
        saveGifToInternalStorageInteractor = SaveGifToInternalStorageInteractor(
            cacheProvider = cacheProvider,
            versionProvider = RealVersionProvider()
        )
    }

    @Test
    fun verifyBytesAreSavedToInternalStorage() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Execute the use-case
        val emissions = saveGifToInternalStorageInteractor.execute(
            contentResolver = contentResolver,
            bytes = byteArray
        ).toList()
        val uri = (emissions[1] as DataState.Data).data
        val file = uri?.toFile()

        // Confirm the file exists in the cache
        assertThat(cacheProvider.gifCache().listFiles().size, equalTo(1))
        assertThat(cacheProvider.gifCache().listFiles()[0].name, equalTo(file?.name))

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(Loading<Uri>(Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Data<Uri>(uri)))
        assertThat(emissions[2], equalTo(Loading<Uri>(Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyErrorEmissionIfOutputStreamFails() = runTest {
        val context = RuntimeEnvironment.getApplication()

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Execute the use-case
        val emissions = saveGifToInternalStorageInteractor.execute(
            contentResolver = mock {
                // Force throw exception
                on { openOutputStream(any()) } doThrow FileNotFoundException("Something is busted")
            },
            bytes = byteArray
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(Loading<Uri>(Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>("Something is busted")))
        assertThat(emissions[2], equalTo(Loading<Uri>(Loading.LoadingState.Idle)))
    }

    @Test
    fun verifyErrorEmissionIfNullOutputStream() = runTest {
        val context = RuntimeEnvironment.getApplication()

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Execute the use-case
        val emissions = saveGifToInternalStorageInteractor.execute(
            contentResolver = mock {
                // Force throw exception
                on { openOutputStream(any()) } doReturn null
            },
            bytes = byteArray
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(Loading<Uri>(Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Uri>(SAVE_GIF_TO_INTERNAL_STORAGE_ERROR)))
        assertThat(emissions[2], equalTo(Loading<Uri>(Loading.LoadingState.Idle)))
    }
}







