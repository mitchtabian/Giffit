package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.net.Uri
import com.codingwithmitch.giffit.MainViewModel.Companion.discardCachedGif
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.BuildGifInteractor
import com.codingwithmitch.giffit.interactors.ResizeGifInteractor
import com.codingwithmitch.giffit.util.buildBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ResizeGifInteractorTest {

    private lateinit var resizeGifInteractor: ResizeGifInteractor
    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())
    private val versionProvider = RealVersionProvider()

    // Build some dummy bitmaps to build a gif with.
    private val bitmaps: List<Bitmap> by lazy {
        val bmps: MutableList<Bitmap> = mutableListOf()
        val bitmap = buildBitmap(RuntimeEnvironment.getApplication().resources)
        repeat(5) {
            bmps.add(bitmap)
        }
        bmps.toList()
    }

    @Before
    fun init() {
        resizeGifInteractor = ResizeGifInteractor(
            cacheProvider = cacheProvider,
            versionProvider = versionProvider
        )
    }

    @Test
    fun `resize gif and verify emissions`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Start by actually building a gif so we know what the starting size is.
        val result = BuildGifInteractor.buildGifAndSaveToInternalStorage(
            contentResolver = contentResolver,
            versionProvider = versionProvider,
            cacheProvider = cacheProvider,
            bitmaps = bitmaps
        )
        val originalGifSize = result.gifSize

        // target 50% of original
        val targetSize = originalGifSize.toFloat() * 0.5f

        // Run use-case
        val emissions = resizeGifInteractor.execute(
            contentResolver = contentResolver,
            capturedBitmaps = bitmaps,
            originalGifSize = originalGifSize.toFloat(),
            targetSize = targetSize,
            bilinearFiltering = false, // Need to disable this for unit tests. Does weird stuff.
            discardCachedGif = ::discardCachedGif
        ).toList()

        // First we want to confirm the progress is incrementing correctly.
        // This also indirectly confirms the resizing is working as expected since the
        // progress param would be wrong if it wasn't.
        var previousProgress = 0f
        var progressCount = 0
        for (emission in emissions) {
            if (emission is DataState.Loading) {
                val loadingState = emission.loadingState
                if (loadingState is DataState.Loading.LoadingState.Active) {
                    assert((loadingState.progress ?: 0f) >= previousProgress)
                    previousProgress = loadingState.progress ?: 0f
                    progressCount++
                }
            }
        }

        // Confirm there are at least 8 progress updates. This should be typical when
        // reducing the size by 50%.
        assert(progressCount >= 8)

        // Confirm the final emission is an Idle loading state
        assertThat(emissions.last(), equalTo(DataState.Loading<Int>(DataState.Loading.LoadingState.Idle)))

        // Confirm the second last emission is DataState.Data and is not null.
        val resizedUri = (emissions[emissions.lastIndex - 1] as DataState.Data<Uri>).data
        assert(resizedUri != null)
    }
}











