package com.codingwithmitch.giffit.viewmodel

import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.ui.MainLoadingState.*
import com.codingwithmitch.giffit.ui.MainState.*
import com.codingwithmitch.giffit.ui.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun init() {
        viewModel = MainViewModel(
            ioDispatcher = testDispatcher,
            saveGifToExternalStorage= mock(),
            buildGif = mock(),
            resizeGif= mock(),
            clearGifCache= mock(),
            captureBitmaps= mock()
        )
    }

    @Test
    fun `verify initial state`() {
        assertThat(viewModel.state.value, equalTo(Initial))
        assertThat(viewModel.loadingState.value, equalTo(Standard(Idle)))
        assertThat(viewModel.errorRelay.value, equalTo(setOf()))
        assertThat(viewModel.toastEventRelay.value, equalTo(null))
    }
}






