package com.connex.app.crypto.protocol

import com.connex.app.domain.model.ChatPayload
import com.squareup.moshi.Moshi

class MessageCodec(private val moshi: Moshi) {
    private val envAdapter = moshi.adapter(Envelope::class.java)
    private val payloadAdapter = moshi.adapter(ChatPayload::class.java)

    fun envToJson(env: Envelope): String = envAdapter.toJson(env)
    fun envFromJson(json: String): Envelope = envAdapter.fromJson(json) ?: error("Invalid envelope json")

    fun payloadToJson(payload: ChatPayload): String = payloadAdapter.toJson(payload)
    fun payloadFromJson(json: String): ChatPayload = payloadAdapter.fromJson(json) ?: error("Invalid payload json")
}
