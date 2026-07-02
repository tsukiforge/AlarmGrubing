package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AwakeStatus(
    val userId: String,
    val nickname: String,
    val isAwake: Boolean,
    val timestamp: Long
)
