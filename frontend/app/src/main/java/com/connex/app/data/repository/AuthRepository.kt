package com.connex.app.data.repository

import com.connex.app.core.util.Result
import com.connex.app.crypto.keys.IdentityKeyManager
import com.connex.app.data.local.prefs.SecurePrefs
import com.connex.app.data.remote.api.AuthApi
import com.connex.app.data.remote.api.UserApi
import com.connex.app.data.remote.dto.LoginReq
import com.connex.app.data.remote.dto.PublicKeyUpsertReq
import com.connex.app.data.remote.dto.RegisterReq
import com.connex.app.data.remote.dto.UpdateProfileReq
import com.connex.app.data.remote.dto.UserProfileResp
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val userApi: UserApi,
    private val prefs: SecurePrefs,
    private val identity: IdentityKeyManager
) {
    suspend fun register(username: String, password: String, displayName: String): Result<Unit> {
        return try {
            val resp = api.register(RegisterReq(username, password, displayName))
            prefs.setAuth(resp.accessToken, resp.userId, resp.username)
            runCatching { api.upsertPublicKey(PublicKeyUpsertReq(identity.getPublicKeyPem())) }
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err(mapAuthError("register", t), t)
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val resp = api.login(LoginReq(username, password))
            prefs.setAuth(resp.accessToken, resp.userId, resp.username)
            runCatching { api.upsertPublicKey(PublicKeyUpsertReq(identity.getPublicKeyPem())) }
            Result.Ok(Unit)
        } catch (t: Throwable) {
            Result.Err(mapAuthError("login", t), t)
        }
    }

    fun isAuthed(): Boolean = prefs.isAuthed()
    fun logout() = prefs.clearAuth()

    fun meUserId(): String? = prefs.getUserId()
    fun meUsername(): String? = prefs.getUsername()
    fun accessToken(): String? = prefs.getToken()

    suspend fun fetchProfile(): Result<UserProfileResp> {
        return try {
            Result.Ok(userApi.getProfile())
        } catch (t: Throwable) {
            Result.Err("profile fetch failed", t)
        }
    }

    suspend fun updateDisplayName(displayName: String): Result<UserProfileResp> {
        return try {
            Result.Ok(userApi.updateProfile(UpdateProfileReq(displayName.trim())))
        } catch (t: Throwable) {
            Result.Err("profile update failed", t)
        }
    }

    private fun mapAuthError(action: String, throwable: Throwable): String {
        return when (throwable) {
            is ConnectException,
            is UnknownHostException -> "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해 주세요."
            is SocketTimeoutException -> "서버 응답이 지연되었습니다. 잠시 후 다시 시도해 주세요."
            is HttpException -> {
                when (throwable.code()) {
                    401 -> if (action == "login") {
                        "아이디 또는 비밀번호가 올바르지 않습니다."
                    } else {
                        "인증에 실패했습니다. 다시 시도해 주세요."
                    }
                    409 -> if (action == "register") {
                        "이미 존재하는 아이디입니다."
                    } else {
                        "요청이 충돌했습니다. 다시 시도해 주세요."
                    }
                    else -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                }
            }
            else -> "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        }
    }
}
