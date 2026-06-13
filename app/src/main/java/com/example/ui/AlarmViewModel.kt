package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.db.AppDatabase
import com.example.data.model.Alarm
import com.example.data.repository.AlarmRepository
import com.example.data.repository.FileTransferRepository
import com.example.data.helper.NetworkConnectionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getDatabase(context)
    private val repository = AlarmRepository(db.alarmDao())

    private val sharedPrefs = context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)

    private val notesRepo = com.example.data.repository.NotesRepository(context)
    private val _notes = MutableStateFlow<List<com.example.data.model.Note>>(emptyList())
    val notes: StateFlow<List<com.example.data.model.Note>> = _notes.asStateFlow()

    // File Transfer Components & State
    private val fileTransferRepo = FileTransferRepository(context)
    private val _localFiles = MutableStateFlow<List<File>>(emptyList())
    val localFiles: StateFlow<List<File>> = _localFiles.asStateFlow()

    private val _showSurveyPrompt = MutableStateFlow(false)
    val showSurveyPrompt: StateFlow<Boolean> = _showSurveyPrompt.asStateFlow()

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

    private val _isOfflineGroup = MutableStateFlow(false)
    val isOfflineGroup: StateFlow<Boolean> = _isOfflineGroup.asStateFlow()

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
        _isOfflineGroup.value = sharedPrefs.getBoolean("is_offline_group", false)

        if (grpCode != null) {
            startPolling(grpCode)
        }
        loadNotes()
        loadLocalFiles()
        checkSurveyPrompt()
    }

    fun updateUserName(name: String) {
        if (name.isBlank()) return
        _userName.value = name
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _notes.value = notesRepo.getNotes()
        }
    }

    fun addNote(title: String, content: String, colorHex: String) {
        viewModelScope.launch {
            val newNote = com.example.data.model.Note(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                lastUpdated = System.currentTimeMillis(),
                colorHex = colorHex
            )
            notesRepo.addNote(newNote)
            loadNotes()
            com.example.widget.NoteWidgetProvider.triggerUpdate(context)
        }
    }

    fun updateNote(note: com.example.data.model.Note, title: String, content: String, colorHex: String) {
        viewModelScope.launch {
            val updated = note.copy(
                title = title,
                content = content,
                lastUpdated = System.currentTimeMillis(),
                colorHex = colorHex
            )
            notesRepo.updateNote(updated)
            loadNotes()
            com.example.widget.NoteWidgetProvider.triggerUpdate(context)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            notesRepo.deleteNote(id)
            loadNotes()
            com.example.widget.NoteWidgetProvider.triggerUpdate(context)
        }
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

            // Sync with Cloud if it's an online group alarm
            if (alarm.isGroup && alarm.groupCode != null && !_isOfflineGroup.value) {
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

            // Sync with Cloud if group alarm and online
            if (alarm.isGroup && alarm.groupCode != null && !_isOfflineGroup.value) {
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

    fun updateAlarm(alarm: Alarm, newTitle: String, newHour: Int, newMinute: Int, newDaysOfWeek: String, newRingtone: String) {
        viewModelScope.launch {
            // Cancel current scheduling
            AlarmScheduler.cancel(context, alarm)

            val updated = alarm.copy(
                title = newTitle,
                hour = newHour,
                minute = newMinute,
                daysOfWeek = newDaysOfWeek,
                ringtoneUri = newRingtone,
                isEnabled = true // re-enable it when user edits to ensure it sings
            )

            repository.updateAlarm(updated)
            AlarmScheduler.schedule(context, updated)

            // Push to Cloud if online and group alarm
            if (updated.isGroup && updated.groupCode != null && !_isOfflineGroup.value) {
                _syncState.value = SyncStatus.Syncing
                val success = pushGroupAlarmsCloud(updated.groupCode)
                if (success) {
                    _syncState.value = SyncStatus.Success("Alarm grup berhasil diperbarui")
                } else {
                    _syncState.value = SyncStatus.Error("Gagal memperbarui ke cloud")
                }
            } else if (updated.isGroup) {
                _syncState.value = SyncStatus.Success("Alarm grup offline diperbarui")
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
                    .putBoolean("is_offline_group", false)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = groupName
                _isCreator.value = true
                _isOfflineGroup.value = false
                _syncState.value = SyncStatus.Success("Grup dibuat: $code")

                startPolling(code)
                onResult(true, code)
            } else {
                // Connection failed - create off-line group!
                val offlineName = "$groupName (Offline)"
                sharedPrefs.edit()
                    .putString("joined_group_code", code)
                    .putString("joined_group_name", offlineName)
                    .putBoolean("is_creator", true)
                    .putBoolean("is_offline_group", true)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = offlineName
                _isCreator.value = true
                _isOfflineGroup.value = true
                _syncState.value = SyncStatus.Success("Dibuat offline karena kendala jaringan (Kode: $code)")

                onResult(true, code)
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
                    .putBoolean("is_offline_group", false)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = cloudGroup.name
                _isCreator.value = false
                _isOfflineGroup.value = false

                // Overwrite local copy with cloud alarms & schedule
                repository.syncGroupAlarmsLocal(code, context)

                _syncState.value = SyncStatus.Success("Berhasil bergabung dengan grup ${cloudGroup.name}")
                startPolling(code)
                onResult(true, null)
            } else {
                // Fallback to offline join mode so they are not blocked!
                val offlineName = "Grup Offline $code"
                sharedPrefs.edit()
                    .putString("joined_group_code", code)
                    .putString("joined_group_name", offlineName)
                    .putBoolean("is_creator", false)
                    .putBoolean("is_offline_group", true)
                    .apply()

                _joinedGroupCode.value = code
                _joinedGroupName.value = offlineName
                _isCreator.value = false
                _isOfflineGroup.value = true

                _syncState.value = SyncStatus.Success("Bergabung offline (Grup: $code)")
                onResult(true, null)
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
                .remove("is_offline_group")
                .apply()

            _joinedGroupCode.value = null
            _joinedGroupName.value = ""
            _isCreator.value = false
            _isOfflineGroup.value = false
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

            // Push to Cloud only if online
            if (!_isOfflineGroup.value) {
                _syncState.value = SyncStatus.Syncing
                val success = pushGroupAlarmsCloud(code)
                if (success) {
                    _syncState.value = SyncStatus.Success("Alarm grup berhasil dibagikan")
                } else {
                    _syncState.value = SyncStatus.Error("Gagal mengunggah alarm ke cloud")
                }
            } else {
                _syncState.value = SyncStatus.Success("Alarm grup offline ditambahkan")
            }
        }
    }

    fun syncOfflineGroupToCloud(onResult: (Boolean, String?) -> Unit) {
        val code = _joinedGroupCode.value ?: return
        val currentName = _joinedGroupName.value.replace(" (Offline)", "").replace(" Offline", "")
        viewModelScope.launch {
            _syncState.value = SyncStatus.Syncing
            try {
                if (!NetworkConnectionHelper.isConnected(context)) {
                    _syncState.value = SyncStatus.Error("Hubungan internet terputus")
                    onResult(false, "Tidak ada koneksi internet. Silakan aktifkan WiFi atau Data Seluler Anda.")
                    return@launch
                }

                val existing = repository.fetchGroupFromCloud(code)
                val success = if (existing == null) {
                    repository.createGroupOnCloud(code, currentName)
                } else {
                    true
                }

                if (success) {
                    val pushSucc = pushGroupAlarmsCloud(code)
                    if (pushSucc) {
                        sharedPrefs.edit()
                            .putBoolean("is_offline_group", false)
                            .putString("joined_group_name", currentName)
                            .apply()

                        _isOfflineGroup.value = false
                        _joinedGroupName.value = currentName
                        _syncState.value = SyncStatus.Success("Berhasil menghubungkan grup ke Cloud!")
                        startPolling(code)
                        onResult(true, null)
                    } else {
                        _syncState.value = SyncStatus.Error("Gagal mengunggah data alarm kelompok")
                        onResult(false, "Gagal mengunggah data alarm ke cloud. Silakan coba lagi.")
                    }
                } else {
                    _syncState.value = SyncStatus.Error("Server cloud tidak merespons")
                    onResult(false, "Tidak dapat menghubungkan kamar grup ke Cloud. Silakan periksa jaringan internet Anda atau pastikan URL Server Sinkronisasi di Settings sudah benar.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _syncState.value = SyncStatus.Error("Kesalahan jaringan: ${e.localizedMessage}")
                onResult(false, "Kesalahan koneksi: ${e.localizedMessage}. Silakan periksa koneksi jaringan internet Anda.")
            }
        }
    }

    private suspend fun pushGroupAlarmsCloud(code: String): Boolean {
        val localList = repository.getGroupAlarms(code).first()
        return repository.pushGroupAlarmsToCloud(code, _joinedGroupName.value, localList)
    }

    // --- Polling Realtime Background ---

    private fun startPolling(code: String) {
        if (_isOfflineGroup.value) return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.syncGroupAlarmsLocal(code, context)
                    _syncState.value = SyncStatus.Synced
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncState.value = SyncStatus.Error("Koneksi ke backend terputus")
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // --- File Transfer Operations ---

    fun loadLocalFiles() {
        _localFiles.value = fileTransferRepo.getLocalSharedFiles()
    }

    fun uploadSharedFile(
        uri: Uri,
        onProgress: (String) -> Unit,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            // Generate a random 6-digit PIN
            val randomPin = (100000..999999).random().toString()
            onProgress("Memulai unggahan...")
            val res = fileTransferRepo.uploadFile(uri, randomPin, _userName.value, onProgress)
            if (res.isSuccess) {
                // Copy locally so it shows in history
                try {
                    val cr = context.contentResolver
                    var name = "unggah_$randomPin"
                    cr.query(uri, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && idx != -1) name = cursor.getString(idx)
                    }
                    val target = File(File(context.filesDir, "shared_files"), name)
                    cr.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                loadLocalFiles()
                checkSurveyPrompt()
            }
            onResult(res)
        }
    }

    fun downloadSharedFile(
        code: String,
        onProgress: (String) -> Unit,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val res = fileTransferRepo.downloadFile(code, onProgress)
            if (res.isSuccess) {
                loadLocalFiles()
                checkSurveyPrompt()
            }
            onResult(res)
        }
    }

    fun deleteLocalFile(file: File) {
        fileTransferRepo.deleteLocalFile(file)
        loadLocalFiles()
    }

    fun clearAllLocalFiles() {
        fileTransferRepo.clearAllLocalFiles()
        loadLocalFiles()
    }

    fun checkSurveyPrompt() {
        val sends = sharedPrefs.getInt("file_share_send_count", 0)
        val recvs = sharedPrefs.getInt("file_share_receive_count", 0)
        val surveyShown = sharedPrefs.getBoolean("file_share_survey_shown_v1", false)
        if ((sends > 0 || recvs > 0) && !surveyShown) {
            _showSurveyPrompt.value = true
        }
    }

    fun dismissSurveyPrompt(neverShowAgain: Boolean) {
        _showSurveyPrompt.value = false
        if (neverShowAgain) {
            sharedPrefs.edit().putBoolean("file_share_survey_shown_v1", true).apply()
        }
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
