package com.codingwithmitch.giffit.viewmodel

import android.graphics.Bitmap
import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.interactors.BuildGif
import com.codingwithmitch.giffit.ui.MainState.*
import com.codingwithmitch.giffit.ui.MainViewModel
import com.codingwithmitch.giffit.util.buildBitmap
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
 * [MainViewModel] tests that involve [BuildGif].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BuildGifTest {

    private lateinit var viewModel: MainViewModel
    private val buildGif: BuildGif = mock()
    private val testDispatcher = StandardTestDispatcher()
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
        viewModel = MainViewModel(
            ioDispatcher = testDispatcher,
            saveGifToExternalStorage= mock(),
            buildGif = buildGif,
            resizeGif = mock(),
            clearGifCache= mock(),
            captureBitmaps= mock(),
            versionProvider = mock()
        )
    }

    @Test
    fun `verify builfGif`() {
        val context = RuntimeEnvironment.getApplication()
        viewModel.verifyInitialState()
        val file = File("${RealCacheProvider(context).gifCache().path}/who_cares.gif")
        val uri = file.toUri()
        viewModel.updateState(
            DisplayBackgroundAsset(
                backgroundAssetUri = uri,
                capturingViewBounds = null,
                capturedBitmaps = bitmaps
            )
        )
        val gifSize = 1234
        whenever(
            buildGif.execute(any(), any())
        ).doReturn(
            flow {
                emit(DataState.Loading(Active()))
                delay(500)
                emit(DataState.Data(BuildGif.BuildGifResult(uri = uri, gifSize = gifSize)))
                delay(500)
                emit(DataState.Loading(Idle))
            }
        )
        viewModel.buildGif(context.contentResolver)

        // Advance past first delay
        testDispatcher.scheduler.advanceTimeBy(500)
        verify(buildGif).execute(any(), any())
        assertThat(
            (viewModel.state.value as DisplayBackgroundAsset).loadingState,
            equalTo(Active())
        )

        // Advance past second delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            viewModel.state.value,
            equalTo(
                DisplayGif(
                    gifUri = uri,
                    resizedGifUri = null,
                    originalGifSize = gifSize,
                    adjustedBytes = gifSize,
                    sizePercentage = 100,
                    backgroundAssetUri = uri,
                    capturedBitmaps = bitmaps
                )
            )
        )

        // Advance past third delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            (viewModel.state.value as DisplayGif).loadingState,
            equalTo(Idle)
        )
    }
}





