package com.connex.app.data.repository

import com.connex.app.core.util.Result
import com.connex.app.crypto.keys.IdentityKeyManager
import com.connex.app.crypto.keys.RoomKeyManager
import com.connex.app.data.local.db.dao.ChannelDao
import com.connex.app.data.local.db.dao.RoomDao
import com.connex.app.data.local.db.entity.ChannelEntity
import com.connex.app.data.local.db.entity.RoomEntity
import com.connex.app.data.remote.api.RoomApi
import com.connex.app.data.remote.dto.*
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val api: RoomApi,
    private val roomDao: RoomDao,
    private val channelDao: ChannelDao,
    private val identity: IdentityKeyManager,
    private val keys: RoomKeyManager,
    private val auth: AuthRepository
) {
    suspend fun refreshRooms(): Result<List<RoomEntity>> {
        return try {
            val rooms = api.listRooms().map { RoomEntity(it.roomId, it.title, it.ownerId) }
            roomDao.clearAll()
            roomDao.upsertAll(rooms)
            Result.Ok(rooms)
        } catch (t: Throwable) {
            Result.Err("list rooms failed", t)
        }
    }

    suspend fun searchRooms(query: String): Result<List<RoomDto>> {
        return try {
            Result.Ok(api.searchRooms(query.trim()))
        } catch (t: Throwable) {
            Result.Err("search rooms failed", t)
        }
    }

    suspend fun localRooms(): List<RoomEntity> = roomDao.listAll()

    suspend fun createRoom(title: String): Result<RoomEntity> {
        return try {
            val me = auth.meUserId() ?: return Result.Err("not authed")
            val created = api.createRoom(CreateRoomReq(title))
            val ent = RoomEntity(created.roomId, created.title, created.ownerId)
            roomDao.upsertAll(listOf(ent))

            // Sync Channels
            val channels = api.listChannels(created.roomId).map {
                ChannelEntity(it.channelId, it.roomId, it.name)
            }
            channelDao.clearRoom(created.roomId)
            channelDao.upsertAll(channels)

            // Keys
            if (channels.isNotEmpty()) {
                val ch = channels.first()
                val k = keys.createNewKey()
                keys.setChannelKey(ch.roomId.toString(), ch.id.toString(), k)
                val wrapped = identity.encryptOaepWithPeer(identity.getPublicKey(), k)
                val b64 = Base64.getEncoder().encodeToString(wrapped)
                api.pushChannelKey(RoomKeyPushReq(roomId = ch.roomId, channelId = ch.id, targetUserId = me, encryptedKeyB64 = b64))
            }

            Result.Ok(ent)
        } catch (t: Throwable) {
            Result.Err("create room failed", t)
        }
    }

    suspend fun joinRoom(roomId: String): Result<Unit> {
        return try {
            api.joinRoom(roomId)
            val channels = api.listChannels(roomId).map { ChannelEntity(it.channelId, it.roomId, it.name) }
            val roomIdInt = roomId.toIntOrNull() ?: channels.firstOrNull()?.roomId ?: 0
            channelDao.clearRoom(roomIdInt)
            channelDao.upsertAll(channels)
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("join failed", t)
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            api.leaveRoom(roomId)
            roomId.toIntOrNull()?.let { channelDao.clearRoom(it) }
            roomId.toIntOrNull()?.let { roomDao.deleteById(it) }
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("leave failed", t)
        }
    }

    suspend fun refreshChannels(roomId: String): Result<List<ChannelEntity>> {
        return try {
            val channels = api.listChannels(roomId).map { ChannelEntity(it.channelId, it.roomId, it.name) }
            roomId.toIntOrNull()?.let { channelDao.clearRoom(it) }
            channelDao.upsertAll(channels)
            Result.Ok(channels)
        } catch (t: Throwable) {
            Result.Err("list channels failed", t)
        }
    }

    suspend fun localChannels(roomId: String): List<ChannelEntity> = 
        roomId.toIntOrNull()?.let { channelDao.listByRoom(it) } ?: emptyList()

    suspend fun createChannel(roomId: String, name: String): Result<ChannelEntity> {
        return try {
            val me = auth.meUserId() ?: return Result.Err("not authed")
            val ch = api.createChannel(roomId, CreateChannelReq(name))
            val ent = ChannelEntity(ch.channelId, ch.roomId, ch.name)
            channelDao.upsertAll(listOf(ent))

            val k = keys.createNewKey()
            keys.setChannelKey(roomId, ent.id.toString(), k)
            val wrapped = identity.encryptOaepWithPeer(identity.getPublicKey(), k)
            val b64 = Base64.getEncoder().encodeToString(wrapped)
            api.pushChannelKey(RoomKeyPushReq(roomId = ch.roomId, channelId = ch.channelId, targetUserId = me, encryptedKeyB64 = b64))

            Result.Ok(ent)
        } catch (t: Throwable) {
            Result.Err("create channel failed", t)
        }
    }

    suspend fun ensureChannelKey(roomId: String, channelId: String): Result<Unit> {
        if (keys.getChannelKey(roomId, channelId) != null) return Result.Ok(Unit)
        return try {
            val pulled = api.pullChannelKey(roomId, channelId)
            val key = keys.decryptWrappedKeyB64(pulled.encryptedKeyB64)
            keys.setChannelKey(roomId, channelId, key)
            Result.Ok(Unit)
        } catch (t: Throwable) {
            // Self-heal: If we can't get key, assume it's lost/missing and generate a new one for OURSELVES.
            // This allows us to start chatting. Note: Previous messages from others might remain unreadable.
            // We only do this if pull failed (404 etc).
            try {
                // Double check if we are auth'd
                val me = auth.meUserId()
                if (me != null) {
                    val k = keys.createNewKey()
                    keys.setChannelKey(roomId, channelId, k)
                    val wrapped = identity.encryptOaepWithPeer(identity.getPublicKey(), k)
                    val b64 = Base64.getEncoder().encodeToString(wrapped)
                    api.pushChannelKey(RoomKeyPushReq(roomId = roomId.toInt(), channelId = channelId.toInt(), targetUserId = me, encryptedKeyB64 = b64))
                    Result.Ok(Unit) 
                } else {
                    Result.Err("pull failed and not authed", t)
                }
            } catch (tn: Throwable) {
                Result.Err("pull and self-heal failed", tn)
            }
        }
    }

    suspend fun listMembers(roomId: String): Result<List<MemberResp>> {
        return try {
            Result.Ok(api.listMembers(roomId))
        } catch (t: Throwable) {
            Result.Err("list members failed", t)
        }
    }

    suspend fun listRoles(roomId: String): Result<List<RoleResp>> {
        return try {
            Result.Ok(api.listRoles(roomId))
        } catch (t: Throwable) {
            Result.Err("list roles failed", t)
        }
    }

    suspend fun createRole(roomId: String, name: String, permissionsCsv: String): Result<RoleResp> {
        return try {
            val perms = permissionsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            Result.Ok(api.createRole(roomId, CreateRoleReq(name, perms)))
        } catch (t: Throwable) {
            Result.Err("create role failed", t)
        }
    }

    suspend fun grantRole(roomId: String, userId: String, roleId: String): Result<Unit> {
        return try {
            api.grantRole(roomId, GrantRoleReq(userId, roleId))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("grant role failed", t)
        }
    }

    suspend fun revokeRole(roomId: String, userId: String, roleId: String): Result<Unit> {
        return try {
            api.revokeRole(roomId, RevokeRoleReq(userId, roleId))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("revoke role failed", t)
        }
    }

    suspend fun assignSubAdmin(roomId: String, userId: Int): Result<Unit> {
        return try {
            api.assignSubAdmin(roomId, SubAdminReq(userId))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("assign subadmin failed", t)
        }
    }

    suspend fun revokeSubAdmin(roomId: String, userId: Int): Result<Unit> {
        return try {
            api.revokeSubAdmin(roomId, userId)
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("revoke subadmin failed", t)
        }
    }

    suspend fun banMember(roomId: String, userId: Int): Result<Unit> {
        return try {
            api.banMember(roomId, BanMemberReq(userId))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("ban member failed", t)
        }
    }

    fun meUserId(): String? = auth.meUserId()
}
