package com.codingwithmitch.giffit.viewmodel

import com.codingwithmitch.giffit.domain.DataState.Loading.LoadingState.*
import com.codingwithmitch.giffit.ui.MainLoadingState.*
import com.codingwithmitch.giffit.ui.MainState
import com.codingwithmitch.giffit.ui.MainViewModel
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo

fun MainViewModel.verifyInitialState() {
    assertThat(state.value, equalTo(MainState.Initial))
    assertThat(loadingState.value, equalTo(Standard(Idle)))
    assertThat(errorRelay.value, equalTo(setOf()))
    assertThat(toastEventRelay.value, equalTo(null))
}