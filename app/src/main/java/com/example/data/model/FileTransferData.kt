package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FileTransferData(
    val code: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val fileData: String, // Base64 encoding
    val senderName: String,
    val timestamp: Long
)
