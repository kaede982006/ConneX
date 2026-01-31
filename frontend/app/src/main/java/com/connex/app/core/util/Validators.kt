package com.connex.app.core.util

object Validators {
    fun isValidUsername(v: String): Boolean {
        if (v.length !in 3..32) return false
        return v.all { it.isLetterOrDigit() || it == '_' }
    }
    fun isValidPassword(v: String): Boolean = v.length >= 8
    fun nonBlank(v: String): Boolean = v.trim().isNotEmpty()
}
