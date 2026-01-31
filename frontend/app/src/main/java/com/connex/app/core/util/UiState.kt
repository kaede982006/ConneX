package com.connex.app.core.util

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Ready<T>(val value: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
