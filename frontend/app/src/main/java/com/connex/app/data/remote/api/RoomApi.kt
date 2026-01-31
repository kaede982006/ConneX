package com.connex.app.data.remote.api

import com.connex.app.data.remote.dto.*
import retrofit2.http.*

interface RoomApi {
    @GET("api/v1/rooms")
    suspend fun listRooms(): List<RoomDto>

    @GET("api/v1/rooms/search")
    suspend fun searchRooms(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): List<RoomDto>

    @POST("api/v1/rooms")
    suspend fun createRoom(@Body request: CreateRoomReq): RoomDto

    @POST("api/v1/rooms/{roomId}/join")
    suspend fun joinRoom(@Path("roomId") roomId: String)

    @POST("api/v1/rooms/{roomId}/leave")
    suspend fun leaveRoom(@Path("roomId") roomId: String)

    @GET("api/v1/rooms/{roomId}/channels")
    suspend fun listChannels(@Path("roomId") roomId: Int): List<ChannelDto>
    // Overload for String roomId if needed, but Repo passes Int usually.
    // Repo passes `created.roomId` (Int) or `roomId` (String)? 
    // Repo `joinRoom(roomId: String)` calls `listChannels(roomId)`. So need String overload or unification.
    @GET("api/v1/rooms/{roomId}/channels")
    suspend fun listChannels(@Path("roomId") roomId: String): List<ChannelDto>

    @POST("api/v1/rooms/{roomId}/channels")
    suspend fun createChannel(@Path("roomId") roomId: String, @Body request: CreateChannelReq): ChannelDto

    @GET("api/v1/rooms/{roomId}/channels/{channelId}/messages")
    suspend fun listMessages(
        @Path("roomId") roomId: String,
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int = 50
    ): List<MessageResp>


    @POST("api/v1/keys/push")
    suspend fun pushChannelKey(@Body request: RoomKeyPushReq)

    @GET("api/v1/keys/pull")
    suspend fun pullChannelKey(@Query("roomId") roomId: String, @Query("channelId") channelId: String): EncryptedKeyResp

    @GET("api/v1/rooms/{roomId}/members")
    suspend fun listMembers(@Path("roomId") roomId: String): List<MemberResp>

    @GET("api/v1/rooms/{roomId}/roles")
    suspend fun listRoles(@Path("roomId") roomId: String): List<RoleResp>

    @POST("api/v1/rooms/{roomId}/roles")
    suspend fun createRole(@Path("roomId") roomId: String, @Body req: CreateRoleReq): RoleResp

    @POST("api/v1/rooms/{roomId}/roles/grant")
    suspend fun grantRole(@Path("roomId") roomId: String, @Body req: GrantRoleReq)

    @POST("api/v1/rooms/{roomId}/roles/revoke")
    suspend fun revokeRole(@Path("roomId") roomId: String, @Body req: RevokeRoleReq)

    @POST("api/v1/rooms/{roomId}/subadmins")
    suspend fun assignSubAdmin(@Path("roomId") roomId: String, @Body req: SubAdminReq)

    @DELETE("api/v1/rooms/{roomId}/subadmins/{userId}")
    suspend fun revokeSubAdmin(@Path("roomId") roomId: String, @Path("userId") userId: Int)

    @POST("api/v1/rooms/{roomId}/bans")
    suspend fun banMember(@Path("roomId") roomId: String, @Body req: BanMemberReq)
}
