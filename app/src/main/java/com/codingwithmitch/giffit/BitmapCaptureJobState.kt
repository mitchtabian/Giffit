package com.codingwithmitch.giffit

sealed class BitmapCaptureJobState {
        
        object Running: BitmapCaptureJobState()
        
        object Idle: BitmapCaptureJobState()
    }