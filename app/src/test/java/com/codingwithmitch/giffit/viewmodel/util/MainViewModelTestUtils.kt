package com.codingwithmitch.giffit.viewmodel

import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.ui.MainState
import com.codingwithmitch.giffit.ui.MainViewModel
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo

fun MainViewModel.verifyInitialState() {
    assert(state.value is MainState.Initial)
    assertThat((state.value as MainState.Initial).loadingState, equalTo(Active()))
    assertThat(errorRelay.value, equalTo(setOf()))
    assertThat(toastEventRelay.value, equalTo(null))
}