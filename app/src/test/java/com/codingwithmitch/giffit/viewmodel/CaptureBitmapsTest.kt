package com.codingwithmitch.giffit.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.interactors.CaptureBitmaps
import com.codingwithmitch.giffit.ui.MainLoadingState.*
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
 * [MainViewModel] tests that involve [CAptureBitmaps].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureBitmapsTest {

    private lateinit var viewModel: MainViewModel
    private val captureBitmaps: CaptureBitmaps = mock()
    private val testDispatcher = StandardTestDispatcher()
    // Build some dummy bitmaps to return from CaptureBitmapsInteractor
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
            buildGif = mock(),
            resizeGif = mock(),
            clearGifCache= mock(),
            captureBitmaps= captureBitmaps
        )
    }

    @Test
    fun `verify captureBitmaps`() {
        val context = RuntimeEnvironment.getApplication()
        viewModel.verifyInitialState()
        val file = File("${RealCacheProvider(context).gifCache().path}/who_cares.gif")
        val uri = file.toUri()
        val mainState = DisplayBackgroundAsset(
            backgroundAssetUri = uri,
            capturingViewBounds = null,
            capturedBitmaps = listOf()
        )
        viewModel.state.value = mainState
        whenever(
            captureBitmaps.execute(any(), any(), any())
        ).doReturn(
            flow {
                emit(DataState.Loading(Active()))
                delay(500)
                emit(DataState.Data(bitmaps))
                delay(500)
                emit(DataState.Loading(Idle))
            }
        )
        viewModel.runBitmapCaptureJob(
            contentResolver = context.contentResolver,
            capturingViewBounds = Rect(1f, 2f ,1f, 2f),
            window = mock(),
            view = mock()
        )

        // Advance past first delay
        testDispatcher.scheduler.advanceTimeBy(500)
        verify(captureBitmaps).execute(any(), any(), any())
        assertThat(
            viewModel.loadingState.value,
            equalTo(BitmapCapture(Active(0.0f)))
        )

        // Advance past second delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            viewModel.state.value,
            equalTo(
                mainState.copy(capturedBitmaps = bitmaps)
            )
        )

        // Advance past third delay
        testDispatcher.scheduler.advanceTimeBy(500)
        assertThat(
            viewModel.loadingState.value,
            equalTo(Standard(Idle))
        )
    }
}










