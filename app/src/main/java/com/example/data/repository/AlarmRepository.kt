package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.api.NetworkClient
import com.example.data.db.AlarmDao
import com.example.data.model.Alarm
import com.example.data.model.Group
import com.example.data.model.GroupAlarm
import com.example.data.helper.JsonHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AlarmRepository(private val alarmDao: AlarmDao) {

    fun getPersonalAlarms(): Flow<List<Alarm>> = alarmDao.getPersonalAlarms()
    
    fun getGroupAlarms(groupCode: String): Flow<List<Alarm>> = alarmDao.getGroupAlarms(groupCode)

    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun insertAlarm(alarm: Alarm) = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)
    
    suspend fun deleteAlarmById(id: String) = alarmDao.deleteAlarmById(id)

    // --- Cloud Sync Operations ---

    suspend fun createGroupOnCloud(code: String, name: String): Boolean {
        return try {
            val group = Group(
                code = code,
                name = name,
                lastUpdated = System.currentTimeMillis(),
                alarms = emptyList()
            )
            val json = JsonHelper.groupToJson(group)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "group_$code")
            val response = NetworkClient.api.putValue(url, requestBody)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchGroupFromCloud(code: String): Group? {
        return try {
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "group_$code")
            val response = NetworkClient.api.getValue(url)
            if (response.isSuccessful) {
                val jsonString = response.body()?.string()
                if (jsonString != null && jsonString.trim() != "null") {
                    JsonHelper.jsonToGroup(jsonString)
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun syncGroupAlarmsLocal(code: String, context: Context): Group? {
        val cloudGroup = fetchGroupFromCloud(code)
        if (cloudGroup != null) {
            // Fetch current local group alarms to cancel deleted ones
            val localAlarms = alarmDao.getGroupAlarms(code).first()
            val incomingIds = cloudGroup.alarms.map { it.id }.toSet()

            val alarms = cloudGroup.alarms.map { cloudAlarm ->
                Alarm(
                    id = cloudAlarm.id,
                    title = cloudAlarm.title,
                    hour = cloudAlarm.hour,
                    minute = cloudAlarm.minute,
                    isGroup = true,
                    groupCode = code,
                    isEnabled = cloudAlarm.isEnabled,
                    ringtoneUri = cloudAlarm.ringtoneUri,
                    daysOfWeek = cloudAlarm.daysOfWeek,
                    creatorName = cloudAlarm.creatorName
                )
            }

            // Optimize check: compare sorted lists by id to skip write & UI flash entirely
            val localSorted = localAlarms.sortedBy { it.id }
            val incomingSorted = alarms.sortedBy { it.id }
            val isIdentical = localSorted.size == incomingSorted.size && localSorted.zip(incomingSorted).all { (local, incoming) ->
                local.id == incoming.id &&
                local.title == incoming.title &&
                local.hour == incoming.hour &&
                local.minute == incoming.minute &&
                local.isEnabled == incoming.isEnabled &&
                local.daysOfWeek == incoming.daysOfWeek &&
                local.ringtoneUri == incoming.ringtoneUri &&
                local.creatorName == incoming.creatorName
            }

            if (isIdentical) {
                return cloudGroup
            }

            // Cancel deleted alarms
            localAlarms.forEach { localAlarm ->
                if (!incomingIds.contains(localAlarm.id)) {
                    AlarmScheduler.cancel(context, localAlarm)
                }
            }

            // Overwrite locally using transaction to prevent temporary empty-state emission
            alarmDao.updateGroupAlarms(code, alarms)

            // Reschedule active ones
            alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    AlarmScheduler.schedule(context, alarm)
                } else {
                    AlarmScheduler.cancel(context, alarm)
                }
            }
        }
        return cloudGroup
    }

    suspend fun pushGroupAlarmsToCloud(code: String, groupName: String, localAlarms: List<Alarm>): Boolean {
        return try {
            val groupAlarms = localAlarms.map { localAlarm ->
                GroupAlarm(
                    id = localAlarm.id,
                    title = localAlarm.title,
                    hour = localAlarm.hour,
                    minute = localAlarm.minute,
                    isEnabled = localAlarm.isEnabled,
                    ringtoneUri = localAlarm.ringtoneUri ?: "default",
                    daysOfWeek = localAlarm.daysOfWeek,
                    creatorName = localAlarm.creatorName
                )
            }
            val group = Group(
                code = code,
                name = groupName,
                lastUpdated = System.currentTimeMillis(),
                alarms = groupAlarms
            )
            val json = JsonHelper.groupToJson(group)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "group_$code")
            val response = NetworkClient.api.putValue(url, requestBody)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
