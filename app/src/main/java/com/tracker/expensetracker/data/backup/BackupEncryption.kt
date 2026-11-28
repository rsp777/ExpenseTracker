package com.tracker.expensetracker.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupEncryption {
    private const val KEY_BITS = 256
    private const val ITERATIONS = 600_000
    private const val TAG_BITS = 128
    const val SALT_BYTES = 16
    const val IV_BYTES = 12

    class Encrypted(val salt: ByteArray, val iv: ByteArray, val ciphertext: ByteArray)

    private val random = SecureRandom()
    private val authenticatedHeader = "expense-tracker-backup:1".toByteArray(Charsets.UTF_8)

    fun encrypt(plaintext: ByteArray, password: CharArray): Encrypted {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val ciphertext = cipher(Cipher.ENCRYPT_MODE, password, salt, iv).doFinal(plaintext)
        return Encrypted(salt, iv, ciphertext)
    }

    fun decrypt(encrypted: Encrypted, password: CharArray): ByteArray =
        cipher(Cipher.DECRYPT_MODE, password, encrypted.salt, encrypted.iv)
            .doFinal(encrypted.ciphertext)

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, iv: ByteArray): Cipher {
        require(salt.size == SALT_BYTES && iv.size == IV_BYTES)
        val specification = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
        }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
                updateAAD(authenticatedHeader)
            }
        } finally {
            key.fill(0)
        }
    }
}
