package com.connex.app.crypto.keys

import java.security.KeyStore

class KeyStoreProvider {
    fun androidKeyStore(): KeyStore {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        return ks
    }
}
