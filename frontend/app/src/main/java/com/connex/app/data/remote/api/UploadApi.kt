package com.connex.app.data.remote.api

import com.connex.app.data.remote.dto.UploadResp
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {
    @Multipart
    @POST("api/v1/uploads")
    suspend fun upload(@Part file: MultipartBody.Part): UploadResp
}
