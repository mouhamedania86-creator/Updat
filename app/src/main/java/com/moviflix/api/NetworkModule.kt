package com.moviflix.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.moviflix.BuildConfig
import com.moviflix.crypto.HitvCrypto
import com.moviflix.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    val gson: Gson = GsonBuilder().create()

    fun provideApiService(session: SessionManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(HitvInterceptor(session))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Decrypt an encrypted API data field.
     * The rawAesKey comes from the response header X-Raw-Aes-Key set by HitvInterceptor.
     * This is called after receiving an ApiResponse<String> for encrypted endpoints.
     */
    fun decryptData(encryptedB64: String, rawAesKey: String): String? {
        return HitvCrypto.decryptResponse(encryptedB64, rawAesKey)
    }
}
