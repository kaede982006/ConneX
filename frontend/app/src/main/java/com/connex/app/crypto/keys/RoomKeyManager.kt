package com.connex.app.crypto.keys

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomKeyManager @Inject constructor() {
    fun createNewKey(): ByteArray = ByteArray(32)
    fun setChannelKey(roomId: String, channelId: String, key: ByteArray) {}
    fun getChannelKey(roomId: String, channelId: String): ByteArray? = null
    fun decryptWrappedKeyB64(wrappedB64: String): ByteArray = ByteArray(32)
}
