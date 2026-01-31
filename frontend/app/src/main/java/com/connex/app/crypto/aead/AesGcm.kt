package com.connex.app.crypto.aead

import com.connex.app.crypto.protocol.Envelope
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcm {
    private val rng = SecureRandom()

    fun encrypt(key32: ByteArray, plaintext: ByteArray, aad: ByteArray): Envelope {
        require(key32.size == 32) { "AES-256 key must be 32 bytes" }
        val nonce = ByteArray(12).also { rng.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key32, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        val ct = cipher.doFinal(plaintext)

        val b64 = Base64.getEncoder()
        return Envelope(
            alg = "AES-256-GCM",
            nonceB64 = b64.encodeToString(nonce),
            cipherB64 = b64.encodeToString(ct),
            aadB64 = b64.encodeToString(aad)
        )
    }

    fun decrypt(key32: ByteArray, env: Envelope): ByteArray {
        require(key32.size == 32) { "AES-256 key must be 32 bytes" }
        val b64 = Base64.getDecoder()
        val nonce = b64.decode(env.nonceB64)
        val ct = b64.decode(env.cipherB64)
        val aad = b64.decode(env.aadB64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key32, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ct)
    }
}
