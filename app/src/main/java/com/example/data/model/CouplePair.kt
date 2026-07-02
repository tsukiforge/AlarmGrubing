package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CouplePair(
    val id: String,
    val roomCode: String,
    val partnerA: String,
    val partnerB: String,
    val partnerAName: String = "",
    val partnerBName: String = "",
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val streakA: Int = 0,
    val streakB: Int = 0,
    val lastWakeA: String = "",
    val lastWakeB: String = "",
    val syncBonusToday: Boolean = false,
    val status: String // "pending" (A proposed to B), "active" (B accepted)
)
