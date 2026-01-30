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

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MemberResp>>>(UiState.Idle)
    val state = _state.asStateFlow()

    fun refresh(roomId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repo.listMembers(roomId)) {
                is Result.Ok -> UiState.Ready(r.value)
                is Result.Err -> UiState.Error(r.message)
            }
        }
    }
}
