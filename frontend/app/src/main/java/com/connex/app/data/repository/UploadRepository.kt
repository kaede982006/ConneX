package com.connex.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.connex.app.core.util.Result
import com.connex.app.data.remote.api.UploadApi
import com.connex.app.domain.model.AttachmentPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: UploadApi
) {
    suspend fun upload(uri: Uri): Result<AttachmentPayload> {
        return try {
            val cr = ctx.contentResolver
            val mime = cr.getType(uri) ?: "application/octet-stream"
            val name = queryName(uri) ?: "file.bin"
            val bytes = cr.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: return Result.Err("cannot read file")

            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", name, body)
            val resp = api.upload(part)

            Result.Ok(
                AttachmentPayload(
                    attachmentId = resp.attachmentId,
                    name = resp.name.ifBlank { name },
                    mimeType = resp.mimeType,
                    sizeBytes = resp.sizeBytes,
                    downloadUrl = resp.downloadUrl
                )
            )
        } catch (t: Throwable) {
            Result.Err("upload failed", t)
        }
    }

    private fun queryName(uri: Uri): String? {
        val cr = ctx.contentResolver
        val c = cr.query(uri, null, null, null, null) ?: return null
        c.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            return if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
        }
    }
}
