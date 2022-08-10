package com.codingwithmitch.giffit

import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.BuildGifInteractor
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorageInteractor
import kotlinx.coroutines.Dispatchers.Main
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var mainViewModel: MainViewModel
    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())
    private val versionProvider = RealVersionProvider()
    private val saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
        versionProvider = versionProvider
    )
    private val buildGifInteractor = BuildGifInteractor(
        cacheProvider = cacheProvider,
        versionProvider = versionProvider
    )

//    @Before
//    fun init() {
//        mainViewModel = MainViewModel(
//            ioDispatcher = Main, // It's a test so just do everything on main
//            saveGifToExternalStorageInteractor = saveGifToExternalStorageInteractor,
//            buildGifInteractor = buildGifInteractor,
//
//        )
//    }
}



