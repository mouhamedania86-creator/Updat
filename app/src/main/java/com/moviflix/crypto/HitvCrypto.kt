package com.moviflix.crypto

import android.util.Base64
import com.moviflix.BuildConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object HitvCrypto {

    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    // ── Generate random 16-char AES key ──────────────────────────────────────
    fun generateRawAesKey(): String {
        return (1..16).map { CHARSET.random() }.joinToString("")
    }

    // ── RSA-encrypt the AES key with the server public key ───────────────────
    fun encryptAesKeyWithRsa(rawAesKey: String): String {
        val pemKey = "-----BEGIN PUBLIC KEY-----\n${BuildConfig.RSA_PUBLIC_KEY}\n-----END PUBLIC KEY-----"
        val stripped = pemKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()

        val keyBytes = Base64.decode(stripped, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA", "BC")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(rawAesKey.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    // ── Build the string to sign: sort keys, concat values + currentTime, URL-encode ──
    fun buildDataToSign(params: Map<String, String>, currentTime: String): String {
        val sortedKeys = params.keys.sorted()
        val valuesOnly = sortedKeys
            .map { params[it] ?: "" }
            .filter { it.isNotEmpty() }
            .joinToString("")
        val full = valuesOnly + currentTime
        return URLEncoder.encode(full, "UTF-8")
    }

    // ── AES-ECB encrypt + MD5 hash → sign ────────────────────────────────────
    fun generateSign(rawAesKey: String, dataString: String): String {
        val keyBytes = rawAesKey.toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val dataBytes = dataString.toByteArray(Charsets.UTF_8)
        val encrypted = cipher.doFinal(padPkcs5(dataBytes))
        val b64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)

        return md5(b64).lowercase()
    }

    // ── Decrypt server response with same AES key ─────────────────────────────
    fun decryptResponse(encryptedB64: String, rawAesKey: String): String? {
        return try {
            val keyBytes = rawAesKey.toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val encryptedBytes = Base64.decode(encryptedB64, Base64.DEFAULT)
            val decrypted = cipher.doFinal(encryptedBytes)
            String(unpadPkcs5(decrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    // ── PKCS5 helpers ─────────────────────────────────────────────────────────
    private fun padPkcs5(data: ByteArray): ByteArray {
        val blockSize = 16
        val padding = blockSize - (data.size % blockSize)
        return data + ByteArray(padding) { padding.toByte() }
    }

    private fun unpadPkcs5(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padding = data.last().toInt() and 0xFF
        if (padding < 1 || padding > 16) return data
        return data.copyOf(data.size - padding)
    }

    // ── MD5 ───────────────────────────────────────────────────────────────────
    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
