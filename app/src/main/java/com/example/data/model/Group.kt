package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Group(
    val code: String,
    val name: String,
    val lastUpdated: Long,
    val alarms: List<GroupAlarm>
)

@JsonClass(generateAdapter = true)
data class GroupAlarm(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val ringtoneUri: String,
    val daysOfWeek: String,
    val creatorName: String?
)
