package com.moviflix.api

import com.moviflix.BuildConfig
import com.moviflix.crypto.HitvCrypto
import com.moviflix.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class HitvInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        val currentTime = System.currentTimeMillis().toString()
        val rawAesKey = HitvCrypto.generateRawAesKey()
        val encryptedAesKey = HitvCrypto.encryptAesKeyWithRsa(rawAesKey)

        // Collect all query params for signing
        val params = mutableMapOf<String, String>()
        for (i in 0 until url.querySize) {
            val name = url.queryParameterName(i)
            val value = url.queryParameterValue(i) ?: ""
            params[name] = value
        }

        val dataToSign = HitvCrypto.buildDataToSign(params, currentTime)
        val sign = HitvCrypto.generateSign(rawAesKey, dataToSign)

        val newRequest = originalRequest.newBuilder()
            .header("clienttype", BuildConfig.CLIENT_TYPE)
            .header("versioncode", BuildConfig.VERSION_CODE)
            .header("deviceid", session.deviceId)
            .header("lang", session.language)
            .header("currenttime", currentTime)
            .header("aeskey", encryptedAesKey)
            .header("sign", sign)
            .header("timezone", session.timezone)
            .header("mcc", session.mcc)
            .header("keke", "false")
            .header("geolatitude", "")
            .header("geolongitude", "")
            .header("geoisocode", "")
            .header("geoisoname", "")
            .header("georeliable", "")
            .header("User-Agent", "okhttp/4.12.0")
            .apply {
                val token = session.token
                if (!token.isNullOrEmpty()) {
                    header("token", token)
                }
                val userType = session.userType
                if (!userType.isNullOrEmpty()) {
                    header("usertype", userType)
                }
            }
            .build()

        val response = chain.proceed(newRequest)

        // If response is encrypted, store the rawAesKey in a tag for decryption
        return response.newBuilder()
            .header("X-Raw-Aes-Key", rawAesKey)
            .build()
    }
}
