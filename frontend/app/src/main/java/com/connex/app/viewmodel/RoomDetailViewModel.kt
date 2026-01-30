package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.local.db.entity.ChannelEntity
import com.connex.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val repo: RoomRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<UiState<List<ChannelEntity>>>(UiState.Idle)
    val channels = _channels.asStateFlow()

    fun refresh(roomId: String) {
        viewModelScope.launch {
            _channels.value = UiState.Loading
            val local = repo.localChannels(roomId)
            if (local.isNotEmpty()) _channels.value = UiState.Ready(local)
            when (val r = repo.refreshChannels(roomId)) {
                is Result.Ok -> _channels.value = UiState.Ready(r.value)
                is Result.Err -> _channels.value = UiState.Error(r.message)
            }
        }
    }

    fun createChannel(roomId: String, name: String) {
        viewModelScope.launch {
            when (val r = repo.createChannel(roomId, name)) {
                is Result.Ok -> refresh(roomId)
                is Result.Err -> _channels.value = UiState.Error(r.message)
            }
        }
    }
}
