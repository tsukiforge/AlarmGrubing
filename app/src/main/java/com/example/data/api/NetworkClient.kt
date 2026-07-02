package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var currentBaseUrl = "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/"
    const val BUCKET_ID = "alarmgrup_v4_maichi_76da"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    var api: KvdbApi = buildApi(currentBaseUrl)
        private set

    fun getFullUrl(bucketId: String, key: String): String {
        val baseUrl = currentBaseUrl
        return if (baseUrl.contains("firebasedatabase.app")) {
            // Firebase Realtime Database REST API requires .json suffix at the end
            if (baseUrl.endsWith("/")) {
                "$baseUrl$bucketId/$key.json"
            } else {
                "$baseUrl/$bucketId/$key.json"
            }
        } else {
            // Standard kvdb.io format
            if (baseUrl.endsWith("/")) {
                "$baseUrl$bucketId/$key"
            } else {
                "$baseUrl/$bucketId/$key"
            }
        }
    }

    fun updateBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (currentBaseUrl != sanitized) {
            currentBaseUrl = sanitized
            api = buildApi(sanitized)
        }
    }

    private fun buildApi(url: String): KvdbApi {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .build()
            .create(KvdbApi::class.java)
    }
}
