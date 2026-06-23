package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WakeTrigger(
    val senderName: String,
    val senderId: String? = null,
    val timestamp: Long
)
