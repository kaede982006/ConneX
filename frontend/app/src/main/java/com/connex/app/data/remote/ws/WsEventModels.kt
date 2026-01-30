package com.connex.app.data.remote.ws

import com.connex.app.crypto.protocol.Envelope

sealed class OutgoingWsEvent {
    data class SendMessage(
        val roomId: String,
        val channelId: String,
        val messageId: String,
        val envelope: Envelope
    ) : OutgoingWsEvent()

    data class Typing(
        val roomId: String,
        val channelId: String,
        val isTyping: Boolean
    ) : OutgoingWsEvent()
}

sealed class IncomingWsEvent {
    data class Message(
        val roomId: String,
        val channelId: String,
        val messageId: String,
        val senderId: String,
        val envelope: Envelope,
        val createdAtMs: Long
    ) : IncomingWsEvent()

    data class Typing(
        val roomId: String,
        val channelId: String,
        val senderId: String,
        val isTyping: Boolean
    ) : IncomingWsEvent()

    data class System(val code: String, val message: String) : IncomingWsEvent()
}
