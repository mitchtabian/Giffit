package com.codingwithmitch.giffit.domain

sealed class DataState<T> {

    data class Data<T>(
        val data: T? = null
    ): DataState<T>()

    data class Error<T>(
        val message: String
    ): DataState<T>()

    data class Loading<T>(
        val loadingState: LoadingState
    ): DataState<T>() {
        enum class LoadingState {
            LOADING,
            IDLE
        }
    }
}