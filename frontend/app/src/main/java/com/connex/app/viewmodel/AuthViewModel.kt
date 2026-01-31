package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.remote.dto.UserProfileResp
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

    private val _profile = MutableStateFlow<UiState<UserProfileResp>>(UiState.Idle)
    val profile = _profile.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repo.login(username, password)) {
                is Result.Ok -> UiState.Ready(Unit)
                is Result.Err -> UiState.Error(r.message)
            }
        }
    }

    fun register(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repo.register(username, password, displayName)) {
                is Result.Ok -> UiState.Ready(Unit)
                is Result.Err -> UiState.Error(r.message)
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
    
    fun logout() {
        repo.logout()
        _authState.value = false
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profile.value = UiState.Loading
            _profile.value = when (val r = repo.fetchProfile()) {
                is Result.Ok -> UiState.Ready(r.value)
                is Result.Err -> UiState.Error("profile load failed")
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            _profile.value = UiState.Loading
            _profile.value = when (val r = repo.updateDisplayName(displayName)) {
                is Result.Ok -> UiState.Ready(r.value)
                is Result.Err -> UiState.Error("profile update failed")
            }
        }
    }
    
    fun isAuthed() = repo.isAuthed()
    fun getUsername() = repo.meUsername()
    fun meUserId() = repo.meUserId()
}
