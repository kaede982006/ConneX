package com.connex.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val roomId: String,
    val channelId: String,
    val senderId: String,
    val senderName: String?,
    val envelopeJson: String,
    val createdAtMs: Long
)
