package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isGroup: Boolean,
    val groupCode: String?,
    val isEnabled: Boolean,
    val ringtoneUri: String?, // "default", "custom_1", "custom_2", "custom_3"
    val daysOfWeek: String, // Comma-separated days e.g. "1,2,3,4,5" (1=Mon, 7=Sun) or "" for once
    val isSnoozed: Boolean = false,
    val lastTriggeredEpoch: Long = 0,
    val creatorName: String? = null
)
