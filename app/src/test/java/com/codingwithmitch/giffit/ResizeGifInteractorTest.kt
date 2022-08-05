package com.codingwithmitch.giffit

import android.graphics.Bitmap
import com.codingwithmitch.giffit.MainViewModel.Companion.discardCachedGif
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.BuildGifInteractor
import com.codingwithmitch.giffit.interactors.GetAssetSizeInteractor
import com.codingwithmitch.giffit.interactors.ResizeGifInteractor
import com.codingwithmitch.giffit.interactors.SaveGifToInternalStorageInteractor
import com.codingwithmitch.giffit.util.buildBitmap
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
        repeat(5) {
            bmps.add(buildBitmap(RuntimeEnvironment.getApplication().resources))
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

    // TODO("Continue here I guess")
    // TODO("prob gunna need to refactor resizeGif b/c this the test is dumb")
    @Test
    fun `thingy`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

//        // Start by actually building a gif
//        val buildGifEmissions = buildGif.execute(
//            contentResolver = contentResolver,
//            bitmaps = bitmaps
//        ).toList()
//        val cachedGifUri = (buildGifEmissions[2] as DataState.Data<Uri>).data

//        // Now get the size of the cached gif
//        val assetSizeEmissions = getAssetSize.execute(
//            contentResolver = contentResolver,
//
//        )

        // Estimate the size of the cached gif. It doesn't matter if it's exact.
        // The goal is to verify the process of resizing it.
        val originalGifSize = buildBitmapByteArray(context.resources).size * 5

        val resizeEmissions = resizeGifInteractor.execute(
            contentResolver = contentResolver,
            capturedBitmaps = bitmaps,
            originalGifSize = originalGifSize.toFloat(),
            targetSize = originalGifSize.toFloat() / 2,
            discardCachedGif = ::discardCachedGif
        ).toList()


    }
}











