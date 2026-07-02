package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val lastUpdated: Long,
    val colorHex: String = "#FFB7C5" // soft sakura pink
)
