package com.codingwithmitch.giffit

import com.codingwithmitch.giffit.domain.DataState.Loading.*

sealed class MainLoadingState {

        abstract val loadingState: LoadingState

        /**
         * Shows an indeterminate progress bar occupying the entire view.
         */
        data class Standard(
            override val loadingState: LoadingState
        ): MainLoadingState()

        /**
         * Shows a determinate progress bar occupying the entire view.
         */
        data class ResizingGif(
            override val loadingState: LoadingState
        ): MainLoadingState()

        /**
         * Shows a determine progress bar indicating how long until the gif recording
         *  will auto-stop.
         */
        data class BitmapCapture(
            override val loadingState: LoadingState
        ): MainLoadingState()
    }