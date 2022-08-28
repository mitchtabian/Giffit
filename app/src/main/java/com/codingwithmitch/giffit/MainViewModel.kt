package com.codingwithmitch.giffit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.codingwithmitch.giffit.MainState.*

class MainViewModel : ViewModel() {

    val state: MutableState<MainState> = mutableStateOf(Initial)

}