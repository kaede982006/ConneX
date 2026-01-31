package com.connex.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterReq(
    val username: String,
    val password: String,
    val displayName: String
)

@JsonClass(generateAdapter = true)
data class LoginReq(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class PublicKeyUpsertReq(
    val publicKeyPem: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val accessToken: String,
    val token_type: String,
    val userId: Int,
    val username: String
)

@JsonClass(generateAdapter = true)
data class UserProfileResp(
    val userId: Int,
    val username: String,
    val displayName: String?
)

@JsonClass(generateAdapter = true)
data class UpdateProfileReq(
    val displayName: String
)
