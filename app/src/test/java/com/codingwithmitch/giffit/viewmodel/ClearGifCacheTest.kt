package com.codingwithmitch.giffit.viewmodel

import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.interactors.*
import com.codingwithmitch.giffit.ui.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * [MainViewModel] tests that involve [ClearGifCache].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClearGifCacheTest {
    private lateinit var viewModel: MainViewModel
    private val clearGifCache: ClearGifCache = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun init() {
        viewModel = MainViewModel(
            ioDispatcher = testDispatcher,
            saveGifToExternalStorage= mock(),
            buildGif = mock(),
            resizeGif = mock(),
            clearGifCache= clearGifCache,
            captureBitmaps = mock(),
            versionProvider = mock()
        )
    }

    @Test
    fun `verify clearCachedFiles`() {
        viewModel.verifyInitialState()
        whenever(
            clearGifCache.execute()
        ).thenReturn(
            flow {
                emit(DataState.Loading(Active()))
                emit(DataState.Data(Unit))
                emit(DataState.Loading(Idle))
            }
        )
        viewModel.clearCachedFiles()
        verify(clearGifCache).execute()
        // Clearing the cached files should have no change to the state
        viewModel.verifyInitialState()
    }
}










