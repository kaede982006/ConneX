package com.connex.app.data.remote.api

import com.connex.app.data.local.prefs.SecurePrefs
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val prefs: SecurePrefs
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = prefs.getToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
