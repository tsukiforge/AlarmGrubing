package com.example.ui

import com.example.data.api.NetworkClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FeedbackData(
    val type: FeedbackType,
    val rating: Int,
    val message: String,
    val email: String?,
    val appVersion: String,
    val deviceModel: String,
    val androidVersion: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FeedbackType {
    BUG, SUGGESTION, PRAISE, GENERAL
}

class FeedbackRepository {
    suspend fun submitFeedback(data: FeedbackData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Target path is /feedback.json in Firebase Realtime Database
            val firebaseBaseUrl = "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/"
            val endpointUrl = "${firebaseBaseUrl}feedback.json"
            
            val jsonPayload = """
                {
                    "type": "${data.type.name}",
                    "rating": ${data.rating},
                    "message": ${escapeJsonString(data.message)},
                    "email": ${data.email?.let { "\"${escapeJsonStringContent(it)}\"" } ?: "null"},
                    "appVersion": "${data.appVersion}",
                    "deviceModel": "${data.deviceModel}",
                    "androidVersion": ${data.androidVersion},
                    "timestamp": ${data.timestamp}
                }
            """.trimIndent()

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val response = NetworkClient.api.postValue(endpointUrl, requestBody)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeJsonString(s: String): String {
        return "\"${escapeJsonStringContent(s)}\""
    }

    private fun escapeJsonStringContent(s: String): String {
        val builder = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '/' -> builder.append("\\/")
                '\t' -> builder.append("\\t")
                '\b' -> builder.append("\\b")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                else -> {
                    if (c.code < 0x20) {
                        builder.append(String.format("\\u%04x", c.code))
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        return builder.toString()
    }
}
