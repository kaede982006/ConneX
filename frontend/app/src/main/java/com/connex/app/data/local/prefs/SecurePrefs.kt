package com.connex.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setAuth(token: String, userId: Int, username: String) {
        prefs.edit()
            .putString("access_token", token)
            .putInt("user_id", userId)
            .putString("username", username)
            .putBoolean("is_authed", true)
            .apply()
    }

    fun isAuthed(): Boolean = prefs.getBoolean("is_authed", false)
    fun getUserId(): String? = if (isAuthed()) prefs.getInt("user_id", -1).toString() else null // Using String for generality in repo
    fun getUsername(): String? = prefs.getString("username", null)
    fun getToken(): String? = prefs.getString("access_token", null)
    fun clearAuth() = prefs.edit().clear().apply()
}
