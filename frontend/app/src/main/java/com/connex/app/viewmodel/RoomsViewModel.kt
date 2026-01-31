package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.local.db.entity.RoomEntity
import com.connex.app.data.remote.dto.RoomDto
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

    private val _searchResults = MutableStateFlow<UiState<List<RoomSearchResult>>>(UiState.Idle)
    val searchResults = _searchResults.asStateFlow()

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

    fun searchRooms(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = UiState.Idle
                return@launch
            }
            _searchResults.value = UiState.Loading
            val joined = repo.localRooms().associateBy { it.id }
            _searchResults.value = when (val r = repo.searchRooms(query)) {
                is Result.Ok -> UiState.Ready(r.value.map { it.toSearchResult(joined.containsKey(it.roomId)) })
                is Result.Err -> UiState.Error(r.message)
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = UiState.Idle
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

data class RoomSearchResult(
    val roomId: Int,
    val title: String?,
    val ownerId: Int?,
    val isJoined: Boolean
)

private fun RoomDto.toSearchResult(isJoined: Boolean): RoomSearchResult =
    RoomSearchResult(roomId = roomId, title = title, ownerId = ownerId, isJoined = isJoined)
