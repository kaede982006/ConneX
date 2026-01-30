package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (repo.login(username, password)) {
                is Result.Ok -> UiState.Ready(Unit)
                is Result.Err -> UiState.Error("login failed")
            }
        }
    }

    fun register(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (repo.register(username, password, displayName)) {
                is Result.Ok -> UiState.Ready(Unit)
                is Result.Err -> UiState.Error("register failed")
            }
        }
    }
    private val _authState = MutableStateFlow<Boolean?>(null)
    val authState = _authState.asStateFlow()

    fun checkAuth() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _authState.value = repo.isAuthed()
        }
    }
    
    fun isAuthed() = repo.isAuthed() // Keep for backward compat if needed, or remove.
}
