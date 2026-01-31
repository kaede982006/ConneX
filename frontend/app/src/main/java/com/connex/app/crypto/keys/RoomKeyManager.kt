package com.connex.app.crypto.keys

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomKeyManager @Inject constructor(
    private val prefs: com.connex.app.data.local.prefs.SecurePrefs,
    private val identity: IdentityKeyManager
) {
    fun createNewKey(): ByteArray {
        val k = ByteArray(32)
        java.security.SecureRandom().nextBytes(k)
        return k
    }

    fun setChannelKey(roomId: String, channelId: String, key: ByteArray) {
        val b64 = java.util.Base64.getEncoder().encodeToString(key)
        prefs.saveKey("chan_key_${roomId}_${channelId}", b64)
    }

    fun getChannelKey(roomId: String, channelId: String): ByteArray? {
        val b64 = prefs.getKey("chan_key_${roomId}_${channelId}") ?: return null
        return try {
            java.util.Base64.getDecoder().decode(b64)
        } catch (e: Exception) {
            null
        }
    }

    fun decryptWrappedKeyB64(wrappedB64: String): ByteArray {
        val wrapped = java.util.Base64.getDecoder().decode(wrappedB64)
        return identity.decryptOaep(wrapped)
    }
}
