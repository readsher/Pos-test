package com.postest.app.auth

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

@Singleton
class PinHasher @Inject constructor() {

    private val iterations = 60_000
    private val keyLength = 256

    data class Result(val hash: String, val salt: String)

    fun hash(pin: String, saltB64: String? = null): Result {
        val salt = saltB64?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val hash = skf.generateSecret(spec).encoded
        return Result(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
        )
    }

    fun verify(pin: String, expectedHash: String, saltB64: String): Boolean {
        val computed = hash(pin, saltB64).hash
        // constant-time compare
        if (computed.length != expectedHash.length) return false
        var diff = 0
        for (i in computed.indices) diff = diff or (computed[i].code xor expectedHash[i].code)
        return diff == 0
    }
}
