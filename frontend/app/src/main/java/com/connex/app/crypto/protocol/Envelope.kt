package com.connex.app.crypto.protocol

data class Envelope(
    val alg: String,
    val nonceB64: String,
    val cipherB64: String,
    val aadB64: String
)
