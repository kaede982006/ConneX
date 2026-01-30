package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.local.db.entity.RoomEntity
import com.connex.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val repo: RoomRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<UiState<List<RoomEntity>>>(UiState.Idle)
    val rooms = _rooms.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _rooms.value = UiState.Loading
            val local = repo.localRooms()
            if (local.isNotEmpty()) _rooms.value = UiState.Ready(local)
            when (val r = repo.refreshRooms()) {
                is Result.Ok -> _rooms.value = UiState.Ready(r.value)
                is Result.Err -> _rooms.value = UiState.Error(r.message)
            }
        }
    }

    fun createRoom(title: String) {
        viewModelScope.launch {
            when (val r = repo.createRoom(title)) {
                is Result.Ok -> refresh()
                is Result.Err -> _rooms.value = UiState.Error(r.message)
            }
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            when (val r = repo.joinRoom(roomId)) {
                is Result.Ok -> refresh()
                is Result.Err -> _rooms.value = UiState.Error(r.message)
            }
        }
    }

    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            when (val r = repo.leaveRoom(roomId)) {
                is Result.Ok -> refresh()
                is Result.Err -> _rooms.value = UiState.Error(r.message)
            }
        }
    }
}
