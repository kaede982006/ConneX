package com.connex.app.core.util

sealed class Result<out T> {
    data class Ok<out T>(val value: T) : Result<T>()
    data class Err(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}
