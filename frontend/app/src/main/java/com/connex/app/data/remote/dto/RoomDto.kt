package com.connex.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRoomReq(
    val title: String
)

@JsonClass(generateAdapter = true)
data class RoomDto(
    val roomId: Int,
    val title: String?,
    val ownerId: Int?
)

@JsonClass(generateAdapter = true)
data class CreateChannelReq(
    val name: String
)

@JsonClass(generateAdapter = true)
data class ChannelDto(
    val channelId: Int,
    val roomId: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class RoomKeyPushReq(
    val roomId: Int,
    val channelId: Int,
    val targetUserId: String,
    val encryptedKeyB64: String
)

@JsonClass(generateAdapter = true)
data class EncryptedKeyResp(
    val encryptedKeyB64: String
)

@JsonClass(generateAdapter = true)
data class CreateRoleReq(
    val name: String,
    val permissions: List<String>
)

@JsonClass(generateAdapter = true)
data class RoleResp(
    val id: Int,
    val name: String,
    val permissions: List<String>
)

@JsonClass(generateAdapter = true)
data class GrantRoleReq(
    val userId: String,
    val roleId: String
)

@JsonClass(generateAdapter = true)
data class RevokeRoleReq(
    val userId: String,
    val roleId: String
)

@JsonClass(generateAdapter = true)
data class MemberResp(
    val userId: Int,
    val username: String,
    val displayName: String?,
    val role: String?
)
