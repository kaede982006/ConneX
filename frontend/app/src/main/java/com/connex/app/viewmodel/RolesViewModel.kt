package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.remote.dto.RoleResp
import com.connex.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RolesViewModel @Inject constructor(
    private val repo: RoomRepository
) : ViewModel() {

    private val _roles = MutableStateFlow<UiState<List<RoleResp>>>(UiState.Idle)
    val roles = _roles.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    fun refresh(roomId: String) {
        viewModelScope.launch {
            _roles.value = UiState.Loading
            _roles.value = when (val r = repo.listRoles(roomId)) {
                is Result.Ok -> UiState.Ready(r.value)
                is Result.Err -> UiState.Error(r.message)
            }
        }
    }

    fun createRole(roomId: String, name: String, permissionsCsv: String) {
        viewModelScope.launch {
            when (val r = repo.createRole(roomId, name, permissionsCsv)) {
                is Result.Ok -> refresh(roomId)
                is Result.Err -> _status.value = r.message
            }
        }
    }

    fun grant(roomId: String, userId: String, roleId: String) {
        viewModelScope.launch {
            when (val r = repo.grantRole(roomId, userId, roleId)) {
                is Result.Ok -> _status.value = "granted"
                is Result.Err -> _status.value = r.message
            }
        }
    }

    fun revoke(roomId: String, userId: String, roleId: String) {
        viewModelScope.launch {
            when (val r = repo.revokeRole(roomId, userId, roleId)) {
                is Result.Ok -> _status.value = "revoked"
                is Result.Err -> _status.value = r.message
            }
        }
    }
}
