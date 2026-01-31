package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.remote.dto.MemberResp
import com.connex.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembersUiModel(
    val members: List<MemberResp>,
    val ownerId: Int?,
    val meId: Int?
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MembersUiModel>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun refresh(roomId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            var ownerId = repo.localRooms().firstOrNull { it.id.toString() == roomId }?.ownerId
            if (ownerId == null) {
                when (repo.refreshRooms()) {
                    is Result.Ok -> {
                        ownerId = repo.localRooms().firstOrNull { it.id.toString() == roomId }?.ownerId
                    }
                    is Result.Err -> {
                        ownerId = null
                    }
                }
            }
            val meId = repo.meUserId()?.toIntOrNull()
            _state.value = when (val r = repo.listMembers(roomId)) {
                is Result.Ok -> UiState.Ready(MembersUiModel(r.value, ownerId, meId))
                is Result.Err -> UiState.Error(r.message)
            }
        }
    }

    fun assignSubAdmin(roomId: String, userId: Int) {
        viewModelScope.launch {
            when (repo.assignSubAdmin(roomId, userId)) {
                is Result.Ok -> refresh(roomId)
                is Result.Err -> _state.value = UiState.Error("부방장 지정 실패")
            }
        }
    }

    fun revokeSubAdmin(roomId: String, userId: Int) {
        viewModelScope.launch {
            when (repo.revokeSubAdmin(roomId, userId)) {
                is Result.Ok -> refresh(roomId)
                is Result.Err -> _state.value = UiState.Error("부방장 해제 실패")
            }
        }
    }

    fun banMember(roomId: String, userId: Int) {
        viewModelScope.launch {
            when (repo.banMember(roomId, userId)) {
                is Result.Ok -> refresh(roomId)
                is Result.Err -> _state.value = UiState.Error("멤버 밴 실패")
            }
        }
    }
}
