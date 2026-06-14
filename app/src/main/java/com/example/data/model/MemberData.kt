package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MemberData(
    val userId: String,
    val profileImageBase64: String?,
    val colorHex: String?,
    val lastActive: Long
)
