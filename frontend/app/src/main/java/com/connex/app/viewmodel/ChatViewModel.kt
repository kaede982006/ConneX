package com.connex.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connex.app.core.util.Result
import com.connex.app.core.util.UiState
import com.connex.app.data.local.db.entity.MessageEntity
import com.connex.app.data.repository.ChatRepository
import com.connex.app.data.repository.RoomRepository
import com.connex.app.data.repository.UploadRepository
import com.connex.app.domain.model.ChatPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val roomRepo: RoomRepository,
    private val chatRepo: ChatRepository,
    private val uploadRepo: UploadRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<UiState<List<MessageEntity>>>(UiState.Idle)
    val messages = _messages.asStateFlow()

    private val _typing = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typing = _typing.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    fun connect(roomId: String, channelId: String) {
        viewModelScope.launch {
            when (val r = roomRepo.ensureChannelKey(roomId, channelId)) {
                is Result.Ok -> {
                    chatRepo.connect(roomId, channelId)
                    _messages.value = UiState.Ready(chatRepo.loadCached(roomId, channelId))

                    viewModelScope.launch {
                        chatRepo.incomingMessages(roomId, channelId).collect { msg ->
                            chatRepo.cacheIncoming(msg)
                            _messages.value = UiState.Ready(chatRepo.loadCached(roomId, channelId))
                        }
                    }

                    viewModelScope.launch {
                        chatRepo.typingEvents(roomId, channelId).collect { t ->
                            _typing.value = _typing.value.toMutableMap().apply { put(t.senderId, t.isTyping) }
                        }
                    }
                }
                is Result.Err -> _status.value = r.message
            }
        }
    }

    fun disconnect() {
        chatRepo.disconnect()
    }

    fun sendText(roomId: String, channelId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            when (val r = chatRepo.sendText(roomId, channelId, text.trim())) {
                is Result.Ok -> Unit
                is Result.Err -> _status.value = r.message
            }
        }
    }

    fun setTyping(roomId: String, channelId: String, isTyping: Boolean) {
        chatRepo.sendTyping(roomId, channelId, isTyping)
    }

    fun sendFile(roomId: String, channelId: String, uri: Uri) {
        viewModelScope.launch {
            when (val up = uploadRepo.upload(uri)) {
                is Result.Ok -> {
                    when (val r = chatRepo.sendAttachment(roomId, channelId, up.value)) {
                        is Result.Ok -> Unit
                        is Result.Err -> _status.value = r.message
                    }
                }
                is Result.Err -> _status.value = up.message
            }
        }
    }

    fun decrypt(roomId: String, channelId: String, msg: MessageEntity): ChatPayload {
        return chatRepo.decrypt(roomId, channelId, msg.senderId, msg.messageId, msg.envelopeJson)
    }
}
