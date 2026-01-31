package com.connex.app.data.remote.ws

import com.connex.app.crypto.protocol.Envelope
import com.connex.app.data.local.prefs.SecurePrefs
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpChatSocket @Inject constructor(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val prefs: SecurePrefs,
    private val wsBaseUrl: String
) : ChatSocket {

    private val _events = MutableSharedFlow<IncomingWsEvent>(extraBufferCapacity = 128)
    private var ws: WebSocket? = null

    private val outgoingAdapter = moshi.adapter(OutgoingWire::class.java)
    private val incomingAdapter = moshi.adapter(IncomingWire::class.java)

    override fun connect(roomId: String, channelId: String) {
        disconnect()
        val token = prefs.getToken() ?: return

        val base = wsBaseUrl.toHttpUrlOrNull() ?: return
        val url = base.newBuilder()
            .addPathSegments("ws/chat")
            .addQueryParameter("room_id", roomId)
            .addQueryParameter("channel_id", channelId)
            .addQueryParameter("token", token)
            .build()

        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                val w = incomingAdapter.fromJson(text) ?: return
                when (w.type) {
                    "message" -> {
                        val env = w.envelope ?: return
                        _events.tryEmit(
                            IncomingWsEvent.Message(
                                roomId = w.roomId ?: "",
                                channelId = w.channelId ?: "",
                                messageId = w.messageId ?: "",
                                senderId = w.senderId ?: "",
                                senderName = w.senderName,
                                envelope = env,
                                createdAtMs = w.createdAtMs ?: 0L
                            )
                        )
                    }
                    "typing" -> {
                        _events.tryEmit(
                            IncomingWsEvent.Typing(
                                roomId = w.roomId ?: "",
                                channelId = w.channelId ?: "",
                                senderId = w.senderId ?: "",
                                isTyping = w.isTyping ?: false
                            )
                        )
                    }
                    "system" -> _events.tryEmit(IncomingWsEvent.System(w.code ?: "SYSTEM", w.message ?: ""))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.tryEmit(IncomingWsEvent.System("WS_FAIL", t.message ?: "ws failure"))
            }
        })
    }

    override fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }

    override fun send(event: OutgoingWsEvent) {
        val socket = ws ?: return
        val wire = when (event) {
            is OutgoingWsEvent.SendMessage -> OutgoingWire(
                type = "send_message",
                roomId = event.roomId,
                channelId = event.channelId,
                messageId = event.messageId,
                envelope = event.envelope,
                isTyping = null
            )
            is OutgoingWsEvent.Typing -> OutgoingWire(
                type = "typing",
                roomId = event.roomId,
                channelId = event.channelId,
                messageId = null,
                envelope = null,
                isTyping = event.isTyping
            )
        }
        socket.send(outgoingAdapter.toJson(wire))
    }

    override fun events() = _events.asSharedFlow()

    data class OutgoingWire(
        val type: String,
        val roomId: String,
        val channelId: String,
        val messageId: String? = null,
        val envelope: Envelope? = null,
        val isTyping: Boolean? = null
    )

    data class IncomingWire(
        val type: String,
        val code: String? = null,
        val message: String? = null,
        val roomId: String? = null,
        val channelId: String? = null,
        val messageId: String? = null,
        val senderId: String? = null,
        val senderName: String? = null,
        val envelope: Envelope? = null,
        val createdAtMs: Long? = null,
        val isTyping: Boolean? = null
    )
}
