package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.compose.ui.geometry.Rect
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.VersionProvider
import com.codingwithmitch.giffit.interactors.CaptureBitmapsInteractor
import com.codingwithmitch.giffit.interactors.PixelCopyJob
import com.codingwithmitch.giffit.interactors.PixelCopyJob.PixelCopyJobState.*
import com.codingwithmitch.giffit.util.buildBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureBitmapsInteractorTest {

    private lateinit var captureBitmapsInteractor: CaptureBitmapsInteractor
    private val versionProvider: VersionProvider = mock()
    private val pixelCopyJob: PixelCopyJob = mock()

    // Build some dummy bitmaps to return from interactor.
    private val bitmaps: List<Bitmap> by lazy {
        val bmps: MutableList<Bitmap> = mutableListOf()
        val bitmap = buildBitmap(RuntimeEnvironment.getApplication().resources)
        repeat(5) {
            bmps.add(bitmap)
        }
        bmps.toList()
    }

    @Test
    fun `verify captureBitmapsInteractor API 26+ success`() = runTest {
        captureBitmapsInteractor = CaptureBitmapsInteractor(versionProvider, pixelCopyJob, StandardTestDispatcher())
        val rect = Rect(0f, 0f, 5f, 5f)
        val view: View = mock {
            on { width } doReturn rect.width.toInt()
            on { height } doReturn rect.height.toInt()
        }

        whenever(versionProvider.provideVersion()).thenReturn(Build.VERSION_CODES.O)
        whenever(pixelCopyJob.execute(any(), any(), any())).thenReturn(Done(bitmaps[0]))
        val emissions = captureBitmapsInteractor.execute(
            capturingViewBounds = rect,
            window = mock(),
            view = view
        ).toList()

        verifyBitmapEmissions(emissions)
    }

    /**
     * This test cannot use [runTest] because it switches to the main thread when writing to the canvas.
     */
    @Test
    fun `verify captureBitmapsInteractor API 26- success`() {
        val dispatcher = StandardTestDispatcher()
        captureBitmapsInteractor = CaptureBitmapsInteractor(versionProvider, pixelCopyJob, dispatcher)
        val rect = Rect(0f, 0f, 5f, 5f)
        val view: View = mock {
            on { width } doReturn rect.width.toInt()
            on { height } doReturn rect.height.toInt()
        }

        whenever(versionProvider.provideVersion()).thenReturn(Build.VERSION_CODES.N_MR1)

        CoroutineScope(dispatcher).launch {
            val emissions = captureBitmapsInteractor.execute(
                capturingViewBounds = rect,
                window = mock(), // Not used for API 25 and below
                view = view
            ).toList()

            verifyBitmapEmissions(emissions)
        }
    }

    @Test
    fun `null viewBounds throws Exception`() = runTest {
        captureBitmapsInteractor = CaptureBitmapsInteractor(versionProvider, pixelCopyJob, StandardTestDispatcher())
        val emissions = captureBitmapsInteractor.execute(
            capturingViewBounds = null,
            window = mock(),
            view = mock()
        ).toList()

        assert(emissions[0] == DataState.Loading<List<Bitmap>>(DataState.Loading.LoadingState.Active()))
        assert(emissions[1] == DataState.Error<List<Bitmap>>("Invalid view bounds."))
        assert(emissions[2] == DataState.Loading<List<Bitmap>>(DataState.Loading.LoadingState.Idle))
    }

    @Test
    fun `null view throws Exception`() = runTest {
        captureBitmapsInteractor = CaptureBitmapsInteractor(versionProvider, pixelCopyJob, StandardTestDispatcher())
        val emissions = captureBitmapsInteractor.execute(
            capturingViewBounds = Rect(1f, 1f, 1f, 1f),
            window = mock(),
            view = null
        ).toList()

        assert(emissions[0] == DataState.Loading<List<Bitmap>>(DataState.Loading.LoadingState.Active()))
        assert(emissions[1] == DataState.Error<List<Bitmap>>("Invalid view."))
        assert(emissions[2] == DataState.Loading<List<Bitmap>>(DataState.Loading.LoadingState.Idle))
    }

    private fun verifyBitmapEmissions(emissions: List<DataState<List<Bitmap>>>) {
        // First we want to confirm the progress is incrementing correctly.
        // This also indirectly confirms the capturing of bitmaps is working as expected
        // since the progress param would be wrong if it wasn't.
        var previousProgress = 0f
        var progressCount = 0
        var numBitmapEmissions = 0
        for (emission in emissions) {
            if (emission is DataState.Loading) {
                val loadingState = emission.loadingState
                if (loadingState is DataState.Loading.LoadingState.Active) {
                    assert((loadingState.progress ?: 0f) >= previousProgress)
                    previousProgress = loadingState.progress ?: 0f
                    progressCount++
                }
            }
            if (emission is DataState.Data) {
                emission.data?.let { newBitmaps ->
                    numBitmapEmissions += 1
                    // The number of Data emissions should equal the number of bitmaps in the list.
                    assert(newBitmaps.size == numBitmapEmissions)
                } ?: throw Exception("List of bitmaps cannot be null.")
            }
        }

        // Confirm there are 17 progress updates since [TOTAL_CAPTURE_TIME_MS] = 4000ms
        // and the intervals are [CAPTURE_INTERVAL_MS] = 250ms.
        assertThat(progressCount, equalTo(17))

        // Confirm the final emission is an Idle loading state
        assertThat(emissions.last(), equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))

        // Confirm the second last emission is DataState.Data and is not null.
        val resizedUri = (emissions[emissions.lastIndex - 1] as DataState.Data<List<Bitmap>>).data
        assert(resizedUri != null)
    }
}







