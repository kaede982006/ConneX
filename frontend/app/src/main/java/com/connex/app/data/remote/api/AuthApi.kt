package com.connex.app.data.remote.api

import com.connex.app.data.remote.dto.AuthResponse
import com.connex.app.data.remote.dto.LoginReq
import com.connex.app.data.remote.dto.PublicKeyUpsertReq
import com.connex.app.data.remote.dto.RegisterReq
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterReq): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginReq): AuthResponse

    @POST("api/v1/users/keys")
    suspend fun upsertPublicKey(@Body request: PublicKeyUpsertReq)
}
