package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var currentBaseUrl = "https://kvdb.io/"
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
