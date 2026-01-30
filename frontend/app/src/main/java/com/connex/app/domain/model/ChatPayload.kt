package com.connex.app.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatPayload(
    val type: String,
    val text: String? = null,
    val attachment: AttachmentPayload? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_ATTACHMENT = "attachment"
        const val TYPE_SYSTEM = "system"

        fun text(v: String): ChatPayload = ChatPayload(type = TYPE_TEXT, text = v)
        fun attachment(a: AttachmentPayload): ChatPayload = ChatPayload(type = TYPE_ATTACHMENT, attachment = a)
        fun system(v: String): ChatPayload = ChatPayload(type = TYPE_SYSTEM, text = v)
    }
}

@JsonClass(generateAdapter = true)
data class AttachmentPayload(
    val attachmentId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val downloadUrl: String
)
