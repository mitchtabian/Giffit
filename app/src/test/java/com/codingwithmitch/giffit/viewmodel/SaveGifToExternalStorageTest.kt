package com.codingwithmitch.giffit.viewmodel

import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.ui.MainLoadingState.*
import com.codingwithmitch.giffit.ui.MainState.*
import com.codingwithmitch.giffit.ui.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * [MainViewModel] tests that involve [SaveGifToExternalStorage].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveGifToExternalStorageTest {

    private lateinit var viewModel: MainViewModel
    private val saveGifToExternalStorage: SaveGifToExternalStorage = mock()
    private val clearGifCache: ClearGifCache = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun init() {
        viewModel = MainViewModel(
            ioDispatcher = testDispatcher,
            saveGifToExternalStorage= saveGifToExternalStorage,
            buildGif = mock(),
            resizeGif= mock(),
            clearGifCache= clearGifCache,
            captureBitmaps = mock()
        )
    }

    @Test
    fun `verify saveGifToExternalStorage`() {
        val context = RuntimeEnvironment.getApplication()
        viewModel.verifyInitialState()
        val file = File("${RealCacheProvider(context).gifCache().path}/who_cares.gif")
        val uri = file.toUri()
        whenever(
            saveGifToExternalStorage.execute(any(), any(), any(), any())
        ).doReturn(
            flow {
                emit(DataState.Loading(Active()))
                delay(500)
                emit(DataState.Data(uri))
                delay(500)
                emit(DataState.Loading(Idle))
            }
        )
        viewModel.updateState(
            DisplayGif(
                gifUri = uri,
                resizedGifUri = null,
                originalGifSize = 0,
                adjustedBytes = 0,
                sizePercentage = 100,
                backgroundAssetUri = uri,
                capturedBitmaps = listOf()
            )
        )
        viewModel.saveGif(
            contentResolver = context.contentResolver,
            launchPermissionRequest = {},
            checkFilePermissions = { true }
        )

        // Advance past first delay
        testDispatcher.scheduler.advanceTimeBy(500)
        verify(saveGifToExternalStorage).execute(any(), any(), any(), any())
        assertThat(
            viewModel.loadingState.value,
            equalTo(Standard(Active()))
        )

        // Advance past second delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            viewModel.state.value,
            equalTo(
                DisplayGif(
                    gifUri = uri,
                    resizedGifUri = null,
                    originalGifSize = 0,
                    adjustedBytes = 0,
                    sizePercentage = 100,
                    backgroundAssetUri = uri,
                    capturedBitmaps = listOf()
                )
            )
        )
        assertThat(
            viewModel.toastEventRelay.value?.message,
            equalTo("Saved")
        )

        // Advance past third delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            viewModel.loadingState.value,
            equalTo(Standard(Idle))
        )
        verify(clearGifCache).execute()
        assertThat(
            viewModel.state.value,
            equalTo(
                DisplayBackgroundAsset(
                    backgroundAssetUri = uri,
                )
            )
        )
    }
}







