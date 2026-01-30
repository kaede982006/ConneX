package com.connex.app.domain.model

data class Message(
    val messageId: String,
    val roomId: String,
    val channelId: String,
    val senderId: String,
    val payload: ChatPayload,
    val createdAtMs: Long
)
