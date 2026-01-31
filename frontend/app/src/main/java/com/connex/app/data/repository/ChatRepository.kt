package com.connex.app.data.repository

import com.connex.app.core.util.Result
import com.connex.app.crypto.aead.AesGcm
import com.connex.app.crypto.keys.RoomKeyManager
import com.connex.app.crypto.protocol.MessageCodec
import com.connex.app.data.local.db.dao.MessageDao
import com.connex.app.data.local.db.entity.MessageEntity
import com.connex.app.data.remote.ws.ChatSocket
import com.connex.app.data.remote.ws.IncomingWsEvent
import com.connex.app.data.remote.ws.OutgoingWsEvent
import com.connex.app.domain.model.ChatPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val socket: ChatSocket,
    private val keys: RoomKeyManager,
    private val aes: AesGcm,
    private val codec: MessageCodec,
    private val messageDao: MessageDao,
    private val api: com.connex.app.data.remote.api.RoomApi,
    private val auth: AuthRepository
) {
    private val _system = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val system = _system.asSharedFlow()

    fun connect(roomId: String, channelId: String) {
        socket.connect(roomId, channelId)
    }

    fun disconnect() {
        socket.disconnect()
    }

    fun typingEvents(roomId: String, channelId: String): Flow<IncomingWsEvent.Typing> =
        socket.events().filterIsInstance<IncomingWsEvent.Typing>()
            .filter { it.roomId == roomId && it.channelId == channelId }

    fun incomingMessages(roomId: String, channelId: String): Flow<MessageEntity> =
        socket.events()
            .filterIsInstance<IncomingWsEvent.Message>()
            .filter { it.roomId == roomId && it.channelId == channelId }
            .map { ev ->
                MessageEntity(
                    messageId = ev.messageId,
                    roomId = ev.roomId,
                    channelId = ev.channelId,
                    senderId = ev.senderId,
                    senderName = ev.senderName,
                    envelopeJson = codec.envToJson(ev.envelope),
                    createdAtMs = ev.createdAtMs
                )
            }



    suspend fun fetchRemoteMessages(roomId: String, channelId: String) {
        try {
            val msgs = api.listMessages(roomId, channelId, 50)
            val entities = msgs.map { m ->
                MessageEntity(
                    messageId = m.messageId,
                    roomId = m.roomId.toString(),
                    channelId = m.channelId.toString(),
                    senderId = m.senderId.toString(),
                    senderName = m.senderName,
                    envelopeJson = m.envelope,
                    createdAtMs = m.createdAtMs
                )
            }
            messageDao.upsertAll(entities)
        } catch (e: Exception) {
            // ignore failure, rely on cache
            e.printStackTrace()
        }
    }

    suspend fun cacheIncoming(msg: MessageEntity) {
        messageDao.upsert(msg)
    }

    suspend fun loadCached(roomId: String, channelId: String): List<MessageEntity> =
        messageDao.list(roomId, channelId, 300)

    fun decrypt(roomId: String, channelId: String, senderId: String, messageId: String, envJson: String): ChatPayload {
        val key = keys.getChannelKey(roomId, channelId) ?: return ChatPayload.system("[no-key]")
        val env = codec.envFromJson(envJson)
        return try {
            val aad = aad(roomId, channelId, senderId, messageId)
            val pt = aes.decrypt(key, env)
            codec.payloadFromJson(String(pt, StandardCharsets.UTF_8))
        } catch (_: Throwable) {
            ChatPayload.system("[decrypt-fail]")
        }
    }

    suspend fun sendText(roomId: String, channelId: String, text: String): Result<String> {
        val me = auth.meUserId() ?: return Result.Err("not authed")
        val key = keys.getChannelKey(roomId, channelId) ?: return Result.Err("missing channel key")
        return try {
            val messageId = UUID.randomUUID().toString()
            val payload = ChatPayload.text(text)
            val payloadJson = codec.payloadToJson(payload).toByteArray(StandardCharsets.UTF_8)
            val env = aes.encrypt(key, payloadJson, aad(roomId, channelId, me, messageId))
            socket.send(OutgoingWsEvent.SendMessage(roomId, channelId, messageId, env))
            Result.Ok(messageId)
        } catch (t: Throwable) {
            Result.Err("send failed", t)
        }
    }

    suspend fun sendAttachment(roomId: String, channelId: String, attachment: com.connex.app.domain.model.AttachmentPayload): Result<String> {
        val me = auth.meUserId() ?: return Result.Err("not authed")
        val key = keys.getChannelKey(roomId, channelId) ?: return Result.Err("missing channel key")
        return try {
            val messageId = UUID.randomUUID().toString()
            val payload = ChatPayload.attachment(attachment)
            val payloadJson = codec.payloadToJson(payload).toByteArray(StandardCharsets.UTF_8)
            val env = aes.encrypt(key, payloadJson, aad(roomId, channelId, me, messageId))
            socket.send(OutgoingWsEvent.SendMessage(roomId, channelId, messageId, env))
            Result.Ok(messageId)
        } catch (t: Throwable) {
            Result.Err("send attachment failed", t)
        }
    }

    fun sendTyping(roomId: String, channelId: String, isTyping: Boolean) {
        socket.send(OutgoingWsEvent.Typing(roomId, channelId, isTyping))
    }

    private fun aad(roomId: String, channelId: String, senderId: String, messageId: String): ByteArray =
        "$roomId|$channelId|$senderId|$messageId".toByteArray(StandardCharsets.UTF_8)
}
