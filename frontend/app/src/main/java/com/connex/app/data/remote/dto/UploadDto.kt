package com.connex.app.data.remote.dto

data class UploadResp(
    val attachmentId: String,
    val downloadUrl: String,
    val mimeType: String,
    val sizeBytes: Long,
    val name: String
)
