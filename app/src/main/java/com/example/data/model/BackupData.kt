package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val alarms: List<Alarm>,
    val stringPrefs: Map<String, String>,
    val booleanPrefs: Map<String, Boolean>,
    val intPrefs: Map<String, Int>,
    val notesPrefs: String? 
)
