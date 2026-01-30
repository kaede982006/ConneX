package com.connex.app.data.repository

import com.connex.app.core.util.Result
import com.connex.app.crypto.keys.IdentityKeyManager
import com.connex.app.data.local.prefs.SecurePrefs
import com.connex.app.data.remote.api.AuthApi
import com.connex.app.data.remote.dto.LoginReq
import com.connex.app.data.remote.dto.PublicKeyUpsertReq
import com.connex.app.data.remote.dto.RegisterReq
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val prefs: SecurePrefs,
    private val identity: IdentityKeyManager
) {
    suspend fun register(username: String, password: String, displayName: String): Result<Unit> {
        return try {
            val resp = api.register(RegisterReq(username, password, displayName))
            prefs.setAuth(resp.accessToken, resp.userId, resp.username)
            api.upsertPublicKey(PublicKeyUpsertReq(identity.getPublicKeyPem()))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("register failed", t)
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val resp = api.login(LoginReq(username, password))
            prefs.setAuth(resp.accessToken, resp.userId, resp.username)
            api.upsertPublicKey(PublicKeyUpsertReq(identity.getPublicKeyPem()))
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err("login failed", t)
        }
    }

    fun isAuthed(): Boolean = prefs.isAuthed()
    fun logout() = prefs.clearAuth()

    fun meUserId(): String? = prefs.getUserId()
    fun meUsername(): String? = prefs.getUsername()
}
