package com.connex.app.data.remote.api

import com.connex.app.data.remote.dto.UpdateProfileReq
import com.connex.app.data.remote.dto.UserProfileResp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getProfile(): UserProfileResp

    @PATCH("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileReq): UserProfileResp
}
