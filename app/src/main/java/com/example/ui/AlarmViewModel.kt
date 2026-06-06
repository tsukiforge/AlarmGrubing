package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.db.AppDatabase
import com.example.data.model.Alarm
import com.example.data.repository.AlarmRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(context)
    private val repository = AlarmRepository(db.alarmDao())

    private val sharedPrefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)

    // User details states
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _joinedGroupCode = MutableStateFlow<String?>(null)
    val joinedGroupCode: StateFlow<String?> = _joinedGroupCode.asStateFlow()

    private val _joinedGroupName = MutableStateFlow("")
    val joinedGroupName: StateFlow<String> = _joinedGroupName.asStateFlow()

    private val _isCreator = MutableStateFlow(false)
    val isCreator: StateFlow<Boolean> = _isCreator.asStateFlow()

    // Sync State
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    // Alarms lists
    val personalAlarms: StateFlow<List<Alarm>> = repository.getPersonalAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupAlarms: StateFlow<List<Alarm>> = repository.getAllAlarms()
        .map { list ->
            val currentCode = _joinedGroupCode.value
            list.filter { it.isGroup && it.groupCode == currentCode }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollJob: Job? = null

    init {
        // Load details or seed
        var uid = sharedPrefs.getString("user_id", null)
        if (uid == null) {
            uid = UUID.randomUUID().toString().take(6)
            sharedPrefs.edit().putString("user_id", uid).apply()
        }
        _userId.value = uid

        var uName = sharedPrefs.getString("user_name", null)
        if (uName == null) {
            uName = "Anggota-$uid"
            sharedPrefs.edit().putString("user_name", uName).apply()
        }
        _userName.value = uName

        val grpCode = sharedPrefs.getString("joined_group_code", null)
        _joinedGroupCode.value = grpCode
        _joinedGroupName.value = sharedPrefs.getString("joined_group_name", "") ?: ""
        _isCreator.value = sharedPrefs.getBoolean("is_creator", false)

        if (grpCode != null) {
            startPolling(grpCode)
        }
    }

    fun updateUserName(name: String) {
        if (name.isBlank()) return
        _userName.value = name
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    // --- Core Personal Alarm Operations ---

    fun addPersonalAlarm(title: String, hour: Int, minute: Int, daysOfWeek: String, ringtone: String) {
        viewModelScope.launch {
            val alarm = Alarm(
                id = UUID.randomUUID().toString(),
                title = title,
                hour = hour,
                minute = minute,
                isGroup = false,
                groupCode = null,
                isEnabled = true,
                ringtoneUri = ringtone,
                daysOfWeek = daysOfWeek,
                creatorName = _userName.value
            )
            repository.insertAlarm(alarm)
            AlarmScheduler.schedule(context, alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmScheduler.schedule(context, updated)
            } else {
                AlarmScheduler.cancel(context, updated)
            }

            // Sync with Cloud if it's a group alarm
            if (alarm.isGroup && alarm.groupCode != null) {
                _syncState.value = SyncStatus.Syncing
                val success = pushGroupAlarmsCloud(alarm.groupCode)
                if (success) {
                    _syncState.value = SyncStatus.Success("Alarm grup diperbarui")
                } else {
                    _syncState.value = SyncStatus.Error("Gagal memperbarui cloud")
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancel(context, alarm)
            repository.deleteAlarm(alarm)

            // Sync with Cloud if group alarm
            if (alarm.isGroup && alarm.groupCode != null) {
                _syncState.value = SyncStatus.Syncing
                val success = pushGroupAlarmsCloud(alarm.groupCode)
                if (success) {
                    _syncState.value = SyncStatus.Success("Alarm grup dihapus")
                } else {
                    _syncState.value = SyncStatus.Error("Gagal memperbarui cloud")
                }
            }
        }
    }

    // --- Core Group Alarm Operations ---

    fun createGroup(groupName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _syncState.value = SyncStatus.Syncing
            var code = ""
            var attempt = 0
            var codeVacant = false

            // Try to find a vacant 4 digit code
            while (!codeVacant && attempt < 5) {
                code = (1000..9999).random().toString()
                val existing = repository.fetchGroupFromCloud(code)
                if (existing == null) {
                    codeVacant = true
                }
                attempt++
            }

            if (!codeVacant) {
                _syncState.value = SyncStatus.Error("Gagal membuat kode grup unik")
                onResult(false, "Gagal mendapatkan kode")
                return@launch
            }

            val success = repository.createGroupOnCloud(code, groupName)
            if (success) {
                sharedPrefs.edit()
                    .putString("joined_group_code", code)
                    .putString("joined_group_name", groupName)
                    .putBoolean("is_creator", true)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = groupName
                _isCreator.value = true
                _syncState.value = SyncStatus.Success("Grup dibuat: $code")

                startPolling(code)
                onResult(true, code)
            } else {
                _syncState.value = SyncStatus.Error("Gagal membuat grup di cloud")
                onResult(false, "Kesalahan jaringan")
            }
        }
    }

    fun joinGroup(code: String, onResult: (Boolean, String?) -> Unit) {
        if (code.length != 4) {
            onResult(false, "Kode harus 4 digit")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncStatus.Syncing
            val cloudGroup = repository.fetchGroupFromCloud(code)
            if (cloudGroup != null) {
                // Save locally
                sharedPrefs.edit()
                    .putString("joined_group_code", code)
                    .putString("joined_group_name", cloudGroup.name)
                    .putBoolean("is_creator", false)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = cloudGroup.name
                _isCreator.value = false

                // Overwrite local copy with cloud alarms & schedule
                repository.syncGroupAlarmsLocal(code, context)

                _syncState.value = SyncStatus.Success("Berhasil bergabung dengan grup ${cloudGroup.name}")
                startPolling(code)
                onResult(true, null)
            } else {
                _syncState.value = SyncStatus.Error("Grup tidak ditemukan")
                onResult(false, "Grup tidak ditemukan")
            }
        }
    }

    fun leaveGroup() {
        stopPolling()
        val code = _joinedGroupCode.value
        viewModelScope.launch {
            if (code != null) {
                // Cancel active alarms in this group
                val localAlarms = repository.getGroupAlarms(code).first()
                localAlarms.forEach {
                    AlarmScheduler.cancel(context, it)
                }
                // Delete locally
                db.alarmDao().deleteGroupAlarms(code)
            }

            sharedPrefs.edit()
                .remove("joined_group_code")
                .remove("joined_group_name")
                .remove("is_creator")
                .apply()

            _joinedGroupCode.value = null
            _joinedGroupName.value = ""
            _isCreator.value = false
            _syncState.value = SyncStatus.Idle
        }
    }

    fun addGroupAlarm(title: String, hour: Int, minute: Int, daysOfWeek: String, ringtone: String) {
        val code = _joinedGroupCode.value ?: return
        viewModelScope.launch {
            val alarm = Alarm(
                id = UUID.randomUUID().toString(),
                title = title,
                hour = hour,
                minute = minute,
                isGroup = true,
                groupCode = code,
                isEnabled = true,
                ringtoneUri = ringtone,
                daysOfWeek = daysOfWeek,
                creatorName = _userName.value
            )
            // Insert local
            repository.insertAlarm(alarm)
            AlarmScheduler.schedule(context, alarm)

            // Push to Cloud
            _syncState.value = SyncStatus.Syncing
            val success = pushGroupAlarmsCloud(code)
            if (success) {
                _syncState.value = SyncStatus.Success("Alarm grup berhasil dibagikan")
            } else {
                _syncState.value = SyncStatus.Error("Gagal mengunggah alarm ke cloud")
            }
        }
    }

    private suspend fun pushGroupAlarmsCloud(code: String): Boolean {
        val localList = repository.getGroupAlarms(code).first()
        return repository.pushGroupAlarmsToCloud(code, _joinedGroupName.value, localList)
    }

    // --- Polling Realtime Background ---

    private fun startPolling(code: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.syncGroupAlarmsLocal(code, context)
                    _syncState.value = SyncStatus.Synced
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    object Synced : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val error: String) : SyncStatus
}
