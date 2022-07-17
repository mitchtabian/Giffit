package com.codingwithmitch.giffit.domain

fun check(isValid: Boolean, message: () -> String) {
    if (!isValid) {
        throw Exception(message())
    }
}