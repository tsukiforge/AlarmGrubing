package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L
)
