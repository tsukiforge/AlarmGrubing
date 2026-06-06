package com.example.data.db

import androidx.room.*
import com.example.data.model.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE isGroup = 0 ORDER BY hour ASC, minute ASC")
    fun getPersonalAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE isGroup = 1 AND groupCode = :groupCode ORDER BY hour ASC, minute ASC")
    fun getGroupAlarms(groupCode: String): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: String): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<Alarm>)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: String)

    @Query("DELETE FROM alarms WHERE isGroup = 1 AND groupCode = :groupCode")
    suspend fun deleteGroupAlarms(groupCode: String)
}
