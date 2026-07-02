package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WakeCooldown(
    val lastSentAt: Long,
    val count: Int,
    val cooldownUntil: Long
)
