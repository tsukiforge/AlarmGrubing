package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.api.NetworkClient

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

    // Group Members state
    private val _groupMembers = MutableStateFlow<List<com.example.data.model.MemberData>>(emptyList())
    val groupMembers: StateFlow<List<com.example.data.model.MemberData>> = _groupMembers.asStateFlow()

    private val _wakeCooldowns = MutableStateFlow<Map<String, com.example.data.model.WakeCooldown>>(emptyMap())
    val wakeCooldowns: StateFlow<Map<String, com.example.data.model.WakeCooldown>> = _wakeCooldowns.asStateFlow()

    // Awake status state
    private val _awakeStatuses = MutableStateFlow<List<com.example.data.model.AwakeStatus>>(emptyList())
    val awakeStatuses: StateFlow<List<com.example.data.model.AwakeStatus>> = _awakeStatuses.asStateFlow()

    // Couple Sync state
    private val _activeCouplePair = MutableStateFlow<com.example.data.model.CouplePair?>(null)
    val activeCouplePair: StateFlow<com.example.data.model.CouplePair?> = _activeCouplePair.asStateFlow()

    private val _pendingCoupleRequests = MutableStateFlow<List<com.example.data.model.CouplePair>>(emptyList())
    val pendingCoupleRequests: StateFlow<List<com.example.data.model.CouplePair>> = _pendingCoupleRequests.asStateFlow()

    // Sync State
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<com.example.data.model.ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<com.example.data.model.ChatMessage>> = _chatMessages.asStateFlow()

    private var lastSentTime = 0L
    private var lastJoinTimestamp = 0L

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
    private var lastSeenAlarmIds: Set<String> = emptySet()

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

        // Auto-sync database changes directly into AlarmManager
        viewModelScope.launch {
            repository.getAllAlarms().collect { alarmList ->
                try {
                    val currentIds = alarmList.map { it.id }.toSet()
                    val deletedIds = lastSeenAlarmIds - currentIds
                    deletedIds.forEach { deletedId ->
                        val dummyAlarm = Alarm(
                            id = deletedId,
                            title = "",
                            hour = 0,
                            minute = 0,
                            isGroup = false,
                            groupCode = null,
                            isEnabled = false,
                            ringtoneUri = null,
                            daysOfWeek = ""
                        )
                        AlarmScheduler.cancel(context, dummyAlarm)
                    }
                    lastSeenAlarmIds = currentIds

                    alarmList.forEach { alarm ->
                        if (alarm.isEnabled) {
                            AlarmScheduler.schedule(context, alarm)
                        } else {
                            AlarmScheduler.cancel(context, alarm)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

                lastJoinTimestamp = System.currentTimeMillis()
                uploadMyMemberProfileInternal(code)
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
                lastJoinTimestamp = System.currentTimeMillis()
                uploadMyMemberProfileInternal(code)
                startPolling(code)
                onResult(true, null)
            } else {
                _syncState.value = SyncStatus.Error("Gagal menemukan grup atau koneksi bermasalah")
                onResult(false, "Kode grup tidak ditemukan atau koneksi internet bermasalah.")
            }
        }
    }

    fun joinGroupViaQr(qrPayload: String, onResult: (Boolean, String?) -> Unit) {
        val parts = qrPayload.split("|")
        if (parts.size != 2) {
            onResult(false, "Format QR Code tidak valid")
            return
        }
        val code = parts[0]
        val timestampStr = parts[1]
        
        val timestamp = timestampStr.toLongOrNull()
        if (timestamp == null) {
            onResult(false, "Format waktu QR Code tidak valid")
            return
        }
        
        val now = System.currentTimeMillis()
        val tenMinutesInMs = 10 * 60 * 1000L
        if (now - timestamp > tenMinutesInMs) {
            onResult(false, "QR Code sudah kedaluwarsa (berlaku maks 10 menit)")
            return
        }
        if (now < timestamp - 60000L) { // Allow 1 minute buffer for clock drift
            onResult(false, "Waktu QR Code tidak sinkron dengan perangkat")
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
                lastJoinTimestamp = System.currentTimeMillis()
                uploadMyMemberProfileInternal(code)
                startPolling(code)
                onResult(true, null)
            } else {
                _syncState.value = SyncStatus.Error("Kamar alarm tidak aktif atau tidak ditemukan di server cloud")
                onResult(false, "Kamar alarm tidak aktif atau tidak ditemukan di server cloud")
            }
        }
    }

    fun kickMember(targetUid: String, onComplete: () -> Unit = {}) {
        val code = _joinedGroupCode.value ?: return
        if (!_isCreator.value) return // Only creator can kick
        
        viewModelScope.launch {
            try {
                // Delete cloud member data
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_${code}/$targetUid")
                NetworkClient.api.deleteValue(url)
                
                // Delete awake status
                val awakeUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_${code}/$targetUid")
                NetworkClient.api.deleteValue(awakeUrl)
                
                // Delete wake triggers
                val triggerUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/$targetUid")
                NetworkClient.api.deleteValue(triggerUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }

    fun leaveGroup() {
        val code = _joinedGroupCode.value
        val uid = _userId.value
        stopPolling()
        viewModelScope.launch {
            if (code != null) {
                // Delete cloud member data immediately
                try {
                    val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_${code}/$uid")
                    NetworkClient.api.deleteValue(url)
                    
                    val awakeUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_${code}/$uid")
                    NetworkClient.api.deleteValue(awakeUrl)
                    
                    val triggerUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/$uid")
                    NetworkClient.api.deleteValue(triggerUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
                        lastJoinTimestamp = System.currentTimeMillis()
                        uploadMyMemberProfileInternal(code)
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

    fun forceSyncGroupAndCouple() {
        val code = _joinedGroupCode.value ?: return
        if (_isOfflineGroup.value) return
        viewModelScope.launch {
            try {
                repository.syncGroupAlarmsLocal(code, context)
                syncAwakeStatusesInternal(code)
                syncCouplePairsInternal(code)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Polling Realtime Background ---

    fun uploadMyMemberProfile() {
        val code = _joinedGroupCode.value ?: return
        if (_isOfflineGroup.value) return
        viewModelScope.launch {
            uploadMyMemberProfileInternal(code)
        }
    }

    suspend fun uploadMyMemberProfileInternal(code: String) {
        val uid = _userId.value
        try {
            // Read profile image if any
            val file = File(context.filesDir, "profile_pic.png")
                val hasCustom = sharedPrefs.getBoolean("has_custom_profile_pic", false) && file.exists()
                var base64: String? = null
                if (hasCustom) {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            val maxDim = 200f
                            val scale = Math.min(maxDim / bmp.width, maxDim / bmp.height)
                            val resized = if (scale < 1f) {
                                android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                            } else bmp
                            val stream = java.io.ByteArrayOutputStream()
                            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, stream)
                            base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Color hex mapping based on Hashcode uniquely
                val colors = listOf("#FFB7B2", "#FFDAC1", "#E2F0CB", "#B5EAD7", "#C7CEEA", "#FFC6FF", "#E8AEB7")
                val idx = Math.abs(uid.hashCode()) % colors.size
                val uColor = colors[idx]

                // Get battery level
                val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                val batteryPct: Float? = batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    level * 100 / scale.toFloat()
                }
                val batteryLvl = batteryPct?.toInt()

                val memberData = com.example.data.model.MemberData(
                    userId = uid,
                    profileImageBase64 = base64,
                    colorHex = uColor,
                    lastActive = System.currentTimeMillis(),
                    batteryLevel = batteryLvl
                )
                
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val mAdapter = moshiObj.adapter(com.example.data.model.MemberData::class.java)
                val json = mAdapter.toJson(memberData)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
                
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_${code}/$uid")
            NetworkClient.api.putValue(url, requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPolling(code: String) {
        if (_isOfflineGroup.value) return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.syncGroupAlarmsLocal(code, context)
                    
                    // Sync awake statuses
                    try {
                        syncAwakeStatusesInternal(code)
                    } catch (eAwake: Exception) {
                        eAwake.printStackTrace()
                    }

                    // Sync couple pairs
                    try {
                        syncCouplePairsInternal(code)
                    } catch (eCouple: Exception) {
                        eCouple.printStackTrace()
                    }

                    // Sync group members
                    try {
                        val memberUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_$code")
                        val response = NetworkClient.api.getValue(memberUrl)
                        if (response.isSuccessful) {
                            val rBodyString = response.body()?.string()
                            if (rBodyString != null && rBodyString.trim() != "null") {
                                val moshiObj = com.squareup.moshi.Moshi.Builder()
                                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                    .build()
                                val type = com.squareup.moshi.Types.newParameterizedType(
                                    Map::class.java,
                                    String::class.java,
                                    com.example.data.model.MemberData::class.java
                                )
                                val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, com.example.data.model.MemberData>> = moshiObj.adapter(type)
                                val membersMap = mapAdapter.fromJson(rBodyString)
                                if (membersMap != null) {
                                    _groupMembers.value = membersMap.values.toList()
                                    if (!_isCreator.value && !membersMap.containsKey(_userId.value)) {
                                        if (System.currentTimeMillis() - lastJoinTimestamp > 25_000L) {
                                            withContext(Dispatchers.Main) { leaveGroup() }
                                            return@launch
                                        } else {
                                            uploadMyMemberProfileInternal(code)
                                        }
                                    } else {
                                        uploadMyMemberProfileInternal(code)
                                    }
                                }
                            } else {
                                if (!_isCreator.value) {
                                    if (System.currentTimeMillis() - lastJoinTimestamp > 25_000L) {
                                        withContext(Dispatchers.Main) { leaveGroup() }
                                        return@launch
                                    } else {
                                        uploadMyMemberProfileInternal(code)
                                    }
                                }
                            }
                        } else if (response.code() == 404 && !_isCreator.value) {
                            if (System.currentTimeMillis() - lastJoinTimestamp > 25_000L) {
                                withContext(Dispatchers.Main) { leaveGroup() }
                                return@launch
                            } else {
                                uploadMyMemberProfileInternal(code)
                            }
                        }
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }

                    // Sync wake cooldowns
                    try {
                        val cooldownUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_cooldown_${code}")
                        val response = NetworkClient.api.getValue(cooldownUrl)
                        if (response.isSuccessful) {
                            val rBodyString = response.body()?.string()
                            if (rBodyString != null && rBodyString.trim() != "null") {
                                val moshiObj = com.squareup.moshi.Moshi.Builder()
                                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                    .build()
                                val type = com.squareup.moshi.Types.newParameterizedType(
                                    Map::class.java,
                                    String::class.java,
                                    com.example.data.model.WakeCooldown::class.java
                                )
                                val adapter: com.squareup.moshi.JsonAdapter<Map<String, com.example.data.model.WakeCooldown>> = moshiObj.adapter(type)
                                val parsed = adapter.fromJson(rBodyString)
                                if (parsed != null) {
                                    _wakeCooldowns.value = parsed
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Check wake triggers for ourselves
                    try {
                        val triggerUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/${_userId.value}")
                        val response = NetworkClient.api.getValue(triggerUrl)
                        if (response.isSuccessful) {
                            val bodyStr = response.body()?.string()
                            if (bodyStr != null && bodyStr.trim() != "null") {
                                val moshiObj = com.squareup.moshi.Moshi.Builder()
                                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                    .build()
                                val adapter = moshiObj.adapter(com.example.data.model.WakeTrigger::class.java)
                                val trigger = adapter.fromJson(bodyStr)
                                if (trigger != null) {
                                    val now = System.currentTimeMillis()
                                    if (now - trigger.timestamp < 45_000L) { // Triggered in the last 45 secs
                                        val serviceIntent = Intent(context, com.example.alarm.AlarmRingingService::class.java).apply {
                                            putExtra("ALARM_ID", "wake_up_trigger_${trigger.timestamp}")
                                            putExtra("ALARM_TITLE", "Bangun Oy!! ⏰ Dibangunkan oleh ${trigger.senderName}")
                                            putExtra("ALARM_TONE", "default")
                                            putExtra("ALARM_IS_GROUP", true)
                                            putExtra("ALARM_GROUP_CODE", code)
                                        }
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    }
                                    
                                    val emptyBody = "null".toRequestBody("application/json".toMediaTypeOrNull())
                                    NetworkClient.api.putValue(triggerUrl, emptyBody)
                                }
                            }
                        }
                    } catch (e3: Exception) {
                        e3.printStackTrace()
                    }

                    // Sync Group Chat
                    try {
                        syncChatMessagesInternal(code)
                    } catch (eChat: Exception) {
                        eChat.printStackTrace()
                    }

                    _syncState.value = SyncStatus.Synced
                } catch (e: Exception) {
                    e.printStackTrace()
                    _syncState.value = SyncStatus.Error("Koneksi ke backend terputus")
                }
                delay(2500) // Poll every 2.5 seconds (high responsiveness)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        _groupMembers.value = emptyList()
        _awakeStatuses.value = emptyList()
        _activeCouplePair.value = null
        _pendingCoupleRequests.value = emptyList()
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

    fun uploadMultipleSharedFiles(
        uris: List<Uri>,
        onProgress: (String) -> Unit,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            onProgress("Mempersiapkan kompresi...")
            val randomPin = (100000..999999).random().toString()
            val tempZipFile = File(context.cacheDir, "UnggahanGrup_v110_$randomPin.zip")
            
            val zipSuccess = fileTransferRepo.zipUris(uris, tempZipFile)
            if (!zipSuccess) {
                onResult(Result.failure(Exception("Gagal membuat paket ZIP dari berkas terpilih")))
                return@launch
            }
            
            onProgress("Mengunggah paket berkas...")
            val zipUri = Uri.fromFile(tempZipFile)
            val res = fileTransferRepo.uploadFile(zipUri, randomPin, _userName.value, onProgress)
            
            if (res.isSuccess) {
                // Copy/extract locally for the user's history row so it displays unzipped files
                onProgress("Mengekstrak paket lokal...")
                try {
                    fileTransferRepo.unzipFile(tempZipFile, File(context.filesDir, "shared_files"), context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                loadLocalFiles()
                checkSurveyPrompt()
            } else {
                tempZipFile.delete()
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

    fun checkWakeCooldown(targetUserId: String): Pair<Boolean, String> {
        val uid = _userId.value
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        
        // Cek limit global per hari (maksimal 2x bangunin siapapun)
        val totalSentToday = _wakeCooldowns.value.entries
            .filter { it.key.startsWith("${uid}_") && it.value.lastSentAt > todayStart }
            .sumOf { it.value.count }
            
        if (totalSentToday >= 2) {
            return Pair(false, "Sudah maksimal membangunkan orang hari ini (2x)")
        }

        val cdKey = "${uid}_${targetUserId}"
        val cd = _wakeCooldowns.value[cdKey]
        if (cd != null) {
            val now = System.currentTimeMillis()
            if (cd.cooldownUntil > now) {
                val remMins = (cd.cooldownUntil - now) / 60000
                val remSecs = ((cd.cooldownUntil - now) % 60000) / 1000
                return Pair(false, String.format("Bisa bangunkan lagi dalam %d:%02d", remMins, remSecs))
            }
        }
        return Pair(true, "")
    }

    fun wakeUpMember(targetUserId: String, onResult: (Boolean) -> Unit) {
        val (allowed, _) = checkWakeCooldown(targetUserId)
        if (!allowed) {
            onResult(false)
            return
        }
        val code = _joinedGroupCode.value ?: return
        val uid = _userId.value
        viewModelScope.launch {
            try {
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                
                val cdKey = "${uid}_${targetUserId}"
                val cd = _wakeCooldowns.value[cdKey]
                val todayStart = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }.timeInMillis
                val newCount = if (cd != null && cd.lastSentAt > todayStart) cd.count + 1 else 1
                val now = System.currentTimeMillis()
                val newCd = com.example.data.model.WakeCooldown(
                    lastSentAt = now,
                    count = newCount,
                    cooldownUntil = now + (5 * 60 * 1000L) // 5 minutes
                )
                val cdAdapter = moshiObj.adapter(com.example.data.model.WakeCooldown::class.java)
                val cdJson = cdAdapter.toJson(newCd)
                val cdBody = cdJson.toRequestBody("application/json".toMediaTypeOrNull())
                val cdUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_cooldown_${code}/$cdKey")
                NetworkClient.api.putValue(cdUrl, cdBody)

                val trigger = com.example.data.model.WakeTrigger(
                    senderName = _userName.value,
                    senderId = uid,
                    timestamp = System.currentTimeMillis()
                )
                val adapter = moshiObj.adapter(com.example.data.model.WakeTrigger::class.java)
                val json = adapter.toJson(trigger)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/$targetUserId")
                val response = NetworkClient.api.putValue(url, requestBody)
                onResult(response.isSuccessful)

                val cooldownUrlResp = NetworkClient.api.getValue(NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_cooldown_${code}"))
                if (cooldownUrlResp.isSuccessful) {
                    val rBodyString = cooldownUrlResp.body()?.string()
                    if (rBodyString != null && rBodyString.trim() != "null") {
                        val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, com.example.data.model.WakeCooldown::class.java)
                        val mAdapter: com.squareup.moshi.JsonAdapter<Map<String, com.example.data.model.WakeCooldown>> = moshiObj.adapter(type)
                        val parsed = mAdapter.fromJson(rBodyString)
                        if (parsed != null) {
                            _wakeCooldowns.value = parsed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun updateMyAwakeStatus(isAwake: Boolean, nickname: String, onResult: (Boolean) -> Unit = {}) {
        val code = _joinedGroupCode.value ?: return
        val uid = _userId.value
        viewModelScope.launch {
            try {
                val status = com.example.data.model.AwakeStatus(
                    userId = uid,
                    nickname = nickname,
                    isAwake = isAwake,
                    timestamp = System.currentTimeMillis()
                )
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshiObj.adapter(com.example.data.model.AwakeStatus::class.java)
                val json = adapter.toJson(status)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Safe and secure node validation: Put only to user's own uid node inside the specific room code node
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_${code}/$uid")
                val response = NetworkClient.api.putValue(url, requestBody)
                
                if (response.isSuccessful) {
                    if (isAwake) {
                        try {
                            val triggerUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/${uid}")
                            val trResp = NetworkClient.api.getValue(triggerUrl)
                            if (trResp.isSuccessful) {
                                val bodyStr = trResp.body()?.string()
                                if (bodyStr != null && bodyStr.trim() != "null") {
                                    val trAdapter = moshiObj.adapter(com.example.data.model.WakeTrigger::class.java)
                                    val trigger = trAdapter.fromJson(bodyStr)
                                    if (trigger != null && trigger.senderId != null && (System.currentTimeMillis() - trigger.timestamp < 5 * 60 * 1000L)) {
                                        val confTrigger = com.example.data.model.WakeTrigger(
                                            senderName = "✅ ${nickname} sudah bangun!",
                                            senderId = uid,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        val cJson = trAdapter.toJson(confTrigger)
                                        val mUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "wake_triggers_${code}/${trigger.senderId}")
                                        NetworkClient.api.putValue(
                                            mUrl,
                                            cJson.toRequestBody("application/json".toMediaTypeOrNull())
                                        )
                                        NetworkClient.api.deleteValue(triggerUrl)
                                    }
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    // Update Couple scoring if active couple exists
                    val activePair = _activeCouplePair.value
                    if (isAwake && activePair != null) {
                        try {
                            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            val yesterdayStr = getYesterdayStr()
                            
                            val isPartnerA = activePair.partnerA == uid
                            val lastWakeSelf = if (isPartnerA) activePair.lastWakeA else activePair.lastWakeB
                            
                            if (lastWakeSelf != todayStr) {
                                // 1. Calculate base points
                                var ptsToAdd = 5 // Standard +5
                                val activeAlarms = groupAlarms.value.filter { it.isEnabled }
                                if (activeAlarms.isNotEmpty()) {
                                    val now = java.util.Calendar.getInstance()
                                    val nowHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                                    val nowMinute = now.get(java.util.Calendar.MINUTE)
                                    
                                    // Check if woke up before or within 5 mins of any enabled alarm
                                    var beforeAlarm = false
                                    var withinFive = false
                                    for (alarm in activeAlarms) {
                                        if (nowHour < alarm.hour || (nowHour == alarm.hour && nowMinute < alarm.minute)) {
                                            beforeAlarm = true
                                        } else if (nowHour == alarm.hour && nowMinute >= alarm.minute && nowMinute <= alarm.minute + 5) {
                                            withinFive = true
                                        }
                                    }
                                    if (beforeAlarm) {
                                        ptsToAdd = 10
                                    } else if (withinFive) {
                                        ptsToAdd = 5
                                    }
                                }
                                
                                // 2. Determine Streak
                                val currentStreak = if (isPartnerA) activePair.streakA else activePair.streakB
                                val newStreak = when (lastWakeSelf) {
                                    yesterdayStr -> currentStreak + 1
                                    todayStr -> currentStreak // No change
                                    else -> 1 // Reset to 1
                                }
                                
                                // Reset Sync Bonus if it's a completely new day (neither partner had updated today yet)
                                val systemNewDayReset = activePair.lastWakeA != todayStr && activePair.lastWakeB != todayStr
                                var newSyncBonus = if (systemNewDayReset) false else activePair.syncBonusToday
                                
                                var bonusA = 0
                                var bonusB = 0
                                
                                val otherUid = if (isPartnerA) activePair.partnerB else activePair.partnerA
                                val otherStatus = _awakeStatuses.value.find { it.userId == otherUid }
                                
                                if (otherStatus != null && otherStatus.isAwake && !newSyncBonus) {
                                    val timeDiff = Math.abs(System.currentTimeMillis() - otherStatus.timestamp)
                                    if (timeDiff < 10 * 60 * 1000L) { // < 10 mins
                                        newSyncBonus = true
                                        bonusA = 15
                                        bonusB = 15
                                    }
                                }
                                
                                // Update other partner's streak if they missed yesterday
                                val lastWakeOther = if (isPartnerA) activePair.lastWakeB else activePair.lastWakeA
                                val otherStreak = if (isPartnerA) activePair.streakB else activePair.streakA
                                val newOtherStreak = if (lastWakeOther != yesterdayStr && lastWakeOther != todayStr) 0 else otherStreak
                                
                                // Build updated CouplePair
                                val updatedPair = if (isPartnerA) {
                                    activePair.copy(
                                        scoreA = activePair.scoreA + ptsToAdd + bonusA,
                                        scoreB = activePair.scoreB + bonusB,
                                        streakA = newStreak,
                                        streakB = newOtherStreak,
                                        lastWakeA = todayStr,
                                        syncBonusToday = newSyncBonus
                                    )
                                } else {
                                    activePair.copy(
                                        scoreB = activePair.scoreB + ptsToAdd + bonusB,
                                        scoreA = activePair.scoreA + bonusA,
                                        streakB = newStreak,
                                        streakA = newOtherStreak,
                                        lastWakeB = todayStr,
                                        syncBonusToday = newSyncBonus
                                    )
                                }
                                
                                // Save to Firebase
                                val pairUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${activePair.id}")
                                val pairAdapter = moshiObj.adapter(com.example.data.model.CouplePair::class.java)
                                val pairJson = pairAdapter.toJson(updatedPair)
                                val pairBody = pairJson.toRequestBody("application/json".toMediaTypeOrNull())
                                try {
                                    val coupleResponse = NetworkClient.api.putValue(pairUrl, pairBody)
                                    if (coupleResponse.isSuccessful) {
                                        _activeCouplePair.value = updatedPair
                                        sharedPrefs.edit().putString("active_couple_json", pairJson).apply()
                                        com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (eScore: Exception) {
                            eScore.printStackTrace()
                        }
                    }
                    syncAwakeStatusesInternal(code)
                }
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    suspend fun syncAwakeStatusesInternal(code: String) {
        if (_isOfflineGroup.value) return
        try {
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_$code")
            val response = NetworkClient.api.getValue(url)
            if (response.isSuccessful) {
                val bodyStr = response.body()?.string()
                if (bodyStr != null && bodyStr.trim() != "null") {
                    val moshiObj = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val type = com.squareup.moshi.Types.newParameterizedType(
                        Map::class.java,
                        String::class.java,
                        com.example.data.model.AwakeStatus::class.java
                    )
                    val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, com.example.data.model.AwakeStatus>> = moshiObj.adapter(type)
                    val map = mapAdapter.fromJson(bodyStr)
                    if (map != null) {
                        val now = System.currentTimeMillis()
                        val oneDayInMs = 24 * 60 * 60 * 1000L
                        
                        // User only sees status of members of the SAME room.
                        // And we only display active ones (less than 24h old)
                        val activeStatuses = map.values.filter {
                            (now - it.timestamp) < oneDayInMs
                        }
                        _awakeStatuses.value = activeStatuses
                        
                        // Auto-cleanup: delete entries older than 24 hours on Firebase Firebase node REST API
                        map.forEach { (key, status) ->
                            if (now - status.timestamp >= oneDayInMs) {
                                val deleteUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_${code}/$key")
                                val emptyBody = "null".toRequestBody("application/json".toMediaTypeOrNull())
                                viewModelScope.launch {
                                    try {
                                        NetworkClient.api.putValue(deleteUrl, emptyBody)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    } else {
                        _awakeStatuses.value = emptyList()
                    }
                } else {
                    _awakeStatuses.value = emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncChatMessagesInternal(code: String) {
        if (_isOfflineGroup.value) return
        try {
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "chat_$code")
            val response = NetworkClient.api.getValue(url)
            if (response.isSuccessful) {
                val bodyStr = response.body()?.string()
                if (bodyStr != null && bodyStr.trim() != "null" && bodyStr.trim().isNotEmpty()) {
                    val moshiObj = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val type = com.squareup.moshi.Types.newParameterizedType(
                        List::class.java,
                        com.example.data.model.ChatMessage::class.java
                    )
                    val listAdapter: com.squareup.moshi.JsonAdapter<List<com.example.data.model.ChatMessage>> = moshiObj.adapter(type)
                    val rawList = listAdapter.fromJson(bodyStr)
                    if (rawList != null) {
                        // Requirements: Pesan otomatis terhapus setelah 7 hari
                        val now = System.currentTimeMillis()
                        val sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L
                        val cleanList = rawList.filter { (now - it.timestamp) < sevenDaysInMs }
                        
                        _chatMessages.value = cleanList
                        
                        // If any expired messages were filtered, sync the cleaned list back to the cloud
                        if (cleanList.size != rawList.size) {
                            val json = listAdapter.toJson(cleanList)
                            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
                            NetworkClient.api.putValue(url, requestBody)
                        }
                    } else {
                        _chatMessages.value = emptyList()
                    }
                } else {
                    _chatMessages.value = emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendChatMessage(text: String, onResult: (Boolean, String?) -> Unit) {
        val code = _joinedGroupCode.value
        if (code == null) {
            onResult(false, "Tidak berada dalam grup manapun")
            return
        }
        if (_isOfflineGroup.value) {
            onResult(false, "Grup offline tidak mendukung chat cloud")
            return
        }
        if (text.trim().isEmpty()) {
            onResult(false, "Pesan tidak boleh kosong")
            return
        }
        
        // Anti-Spam Rate limit: max 1 pesan per 3 detik
        val now = System.currentTimeMillis()
        if (now - lastSentTime < 3000L) {
            val remainSeconds = ((3000L - (now - lastSentTime)) / 1000) + 1
            onResult(false, "Batas pengiriman pesan terlampaui. Harap tunggu $remainSeconds detik lagi.")
            return
        }
        
        lastSentTime = now
        
        viewModelScope.launch {
            try {
                // Sanitize message text: filter kata kasar, email, No HP
                val sanitizedText = com.example.data.helper.ChatSanitizer.sanitize(text)
                
                val currentNick = sharedPrefs.getString("wake_nickname", "") ?: ""
                val senderName = if (currentNick.isNotBlank()) currentNick else _userName.value
                
                val newMessage = com.example.data.model.ChatMessage(
                    id = java.util.UUID.randomUUID().toString().take(6),
                    senderId = _userId.value,
                    senderNickname = senderName,
                    messageText = sanitizedText,
                    timestamp = now
                )
                
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "chat_$code")
                
                // Get existing chat list
                var currentList = emptyList<com.example.data.model.ChatMessage>()
                val response = NetworkClient.api.getValue(url)
                if (response.isSuccessful) {
                    val bodyStr = response.body()?.string()
                    if (bodyStr != null && bodyStr.trim() != "null" && bodyStr.trim().isNotEmpty()) {
                        val moshiObj = com.squareup.moshi.Moshi.Builder()
                            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val type = com.squareup.moshi.Types.newParameterizedType(
                            List::class.java,
                            com.example.data.model.ChatMessage::class.java
                        )
                        val listAdapter: com.squareup.moshi.JsonAdapter<List<com.example.data.model.ChatMessage>> = moshiObj.adapter(type)
                        val parsed = listAdapter.fromJson(bodyStr)
                        if (parsed != null) {
                            currentList = parsed
                        }
                    }
                }
                
                // Append and filter old messages (> 7 days)
                val sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L
                val updatedList = (currentList + newMessage).filter { (now - it.timestamp) < sevenDaysInMs }
                
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val type = com.squareup.moshi.Types.newParameterizedType(
                    List::class.java,
                    com.example.data.model.ChatMessage::class.java
                )
                val listAdapter = moshiObj.adapter<List<com.example.data.model.ChatMessage>>(type)
                
                val json = listAdapter.toJson(updatedList)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
                val writeResponse = NetworkClient.api.putValue(url, requestBody)
                
                if (writeResponse.isSuccessful) {
                    _chatMessages.value = updatedList
                    onResult(true, null)
                } else {
                    onResult(false, "Gagal mengirim pesan ke server")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Kesalahan jaringan: ${e.message}")
            }
        }
    }

    fun closeGroupAndCleanChat() {
        if (!_isCreator.value) return
        val code = _joinedGroupCode.value ?: return
        
        viewModelScope.launch {
            try {
                // Delete chat
                val chatUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "chat_$code")
                val emptyBody = "null".toRequestBody("application/json".toMediaTypeOrNull())
                NetworkClient.api.putValue(chatUrl, emptyBody)
                
                // Delete active statuses inside the room
                val awakeStatusesUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "awake_statuses_$code")
                NetworkClient.api.putValue(awakeStatusesUrl, emptyBody)

                // Delete members lists
                val membersUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "members_$code")
                NetworkClient.api.putValue(membersUrl, emptyBody)
                
                // Delete group configuration on cloud
                val configUrl = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "group_$code")
                NetworkClient.api.putValue(configUrl, emptyBody)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                leaveGroup()
            }
        }
    }

    private fun getYesterdayStr(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    fun sendPairRequest(targetUserId: String, targetUserName: String, onResult: (Boolean) -> Unit) {
        val code = _joinedGroupCode.value ?: return onResult(false)
        
        // Verify target lies within current group members
        val memberExists = _groupMembers.value.any { it.userId == targetUserId } || targetUserId == "offline_partner"
        if (!memberExists) {
            return onResult(false)
        }

        // Allow offline pairing mock
        if (targetUserId == "offline_partner" || _isOfflineGroup.value) {
            val pairId = "couple_${_userId.value}_${targetUserId}"
            val request = com.example.data.model.CouplePair(
                id = pairId,
                roomCode = code,
                partnerA = _userId.value,
                partnerB = targetUserId,
                partnerAName = _userName.value,
                partnerBName = targetUserName,
                status = "pending"
            )
            _pendingCoupleRequests.value = _pendingCoupleRequests.value + request
            onResult(true)
            return
        }

        val pairId = "couple_${_userId.value}_${targetUserId}"
        val request = com.example.data.model.CouplePair(
            id = pairId,
            roomCode = code,
            partnerA = _userId.value,
            partnerB = targetUserId,
            partnerAName = _userName.value,
            partnerBName = targetUserName,
            status = "pending"
        )
        viewModelScope.launch {
            try {
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshiObj.adapter(com.example.data.model.CouplePair::class.java)
                val json = adapter.toJson(request)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${pairId}")
                val response = NetworkClient.api.putValue(url, requestBody)
                
                if (response.isSuccessful) {
                    syncCouplePairsInternal(code)
                }
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun acceptPairRequest(pair: com.example.data.model.CouplePair, onResult: (Boolean) -> Unit) {
        val code = _joinedGroupCode.value ?: return onResult(false)
        val updated = pair.copy(status = "active")
        viewModelScope.launch {
            try {
                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshiObj.adapter(com.example.data.model.CouplePair::class.java)
                val json = adapter.toJson(updated)
                val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${pair.id}")
                val response = NetworkClient.api.putValue(url, requestBody)
                
                if (response.isSuccessful) {
                    syncCouplePairsInternal(code)
                }
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun rejectOrCancelPairRequest(pair: com.example.data.model.CouplePair, onResult: (Boolean) -> Unit) {
        val code = _joinedGroupCode.value ?: return onResult(false)
        viewModelScope.launch {
            try {
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${pair.id}")
                val emptyBody = "null".toRequestBody("application/json".toMediaTypeOrNull())
                val response = NetworkClient.api.putValue(url, emptyBody)
                
                if (response.isSuccessful) {
                    syncCouplePairsInternal(code)
                }
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun unpair(onResult: (Boolean) -> Unit) {
        val pair = _activeCouplePair.value ?: return onResult(false)
        val code = _joinedGroupCode.value ?: return onResult(false)
        viewModelScope.launch {
            try {
                val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_${code}/${pair.id}")
                val emptyBody = "null".toRequestBody("application/json".toMediaTypeOrNull())
                val response = NetworkClient.api.putValue(url, emptyBody)
                
                if (response.isSuccessful) {
                    _activeCouplePair.value = null
                    sharedPrefs.edit().remove("active_couple_json").apply()
                    com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
                    syncCouplePairsInternal(code)
                }
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    suspend fun syncCouplePairsInternal(code: String) {
        if (_isOfflineGroup.value) return
        try {
            val url = NetworkClient.getFullUrl(NetworkClient.BUCKET_ID, "couple_pairs_$code")
            val response = NetworkClient.api.getValue(url)
            if (response.isSuccessful) {
                val bodyStr = response.body()?.string()
                if (bodyStr != null && bodyStr.trim() != "null") {
                    val moshiObj = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val type = com.squareup.moshi.Types.newParameterizedType(
                        Map::class.java,
                        String::class.java,
                        com.example.data.model.CouplePair::class.java
                    )
                    val mapAdapter: com.squareup.moshi.JsonAdapter<Map<String, com.example.data.model.CouplePair>> = moshiObj.adapter(type)
                    val map = mapAdapter.fromJson(bodyStr)
                    if (map != null) {
                        val myUid = _userId.value
                        
                        // Find if we are part of an active pairing
                        val activePair = map.values.find {
                            it.status == "active" && (it.partnerA == myUid || it.partnerB == myUid)
                        }
                        
                        // Find pending requests directed to us or sent by us
                        val pending = map.values.filter {
                            it.status == "pending" && (it.partnerA == myUid || it.partnerB == myUid)
                        }
                        
                        _activeCouplePair.value = activePair
                        _pendingCoupleRequests.value = pending
                        
                        // Persist active couple in shared preferences for widget access
                        if (activePair != null) {
                            val pairAdapter = moshiObj.adapter(com.example.data.model.CouplePair::class.java)
                            val pairJson = pairAdapter.toJson(activePair)
                            sharedPrefs.edit().putString("active_couple_json", pairJson).apply()
                        } else {
                            sharedPrefs.edit().remove("active_couple_json").apply()
                        }
                        
                        // Notify widget to refresh
                        com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
                    } else {
                        _activeCouplePair.value = null
                        _pendingCoupleRequests.value = emptyList()
                        sharedPrefs.edit().remove("active_couple_json").apply()
                        com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
                    }
                } else {
                    _activeCouplePair.value = null
                    _pendingCoupleRequests.value = emptyList()
                    sharedPrefs.edit().remove("active_couple_json").apply()
                    com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
                }
            } else {
                _activeCouplePair.value = null
                _pendingCoupleRequests.value = emptyList()
                sharedPrefs.edit().remove("active_couple_json").apply()
                com.example.widget.CoupleWidgetProvider.triggerUpdate(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportBackup(context: android.content.Context, destUri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val alarms = repository.getAllAlarms().first()
                
                val prefs = context.getSharedPreferences("alarm_grup_prefs", android.content.Context.MODE_PRIVATE)
                val allPrefs = prefs.all

                val stringPrefs = mutableMapOf<String, String>()
                val booleanPrefs = mutableMapOf<String, Boolean>()
                val intPrefs = mutableMapOf<String, Int>()

                for ((key, value) in allPrefs) {
                    when (value) {
                        is String -> stringPrefs[key] = value
                        is Boolean -> booleanPrefs[key] = value
                        is Int -> intPrefs[key] = value
                    }
                }

                val notesPrefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("all_notes", null)

                val backupData = com.example.data.model.BackupData(
                    alarms = alarms,
                    stringPrefs = stringPrefs,
                    booleanPrefs = booleanPrefs,
                    intPrefs = intPrefs,
                    notesPrefs = notesPrefs
                )

                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshiObj.adapter(com.example.data.model.BackupData::class.java)
                val json = adapter.toJson(backupData)

                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(json.toByteArray())
                }

                launch(kotlinx.coroutines.Dispatchers.Main) { onResult(true, "Backup berhasil disimpan!") }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(kotlinx.coroutines.Dispatchers.Main) { onResult(false, e.message ?: "Gagal membuat backup") }
            }
        }
    }

    fun importBackup(context: android.content.Context, srcUri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(srcUri)?.use { input ->
                    input.bufferedReader().use { it.readText() }
                } ?: throw Exception("File gagal dibaca")

                val moshiObj = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshiObj.adapter(com.example.data.model.BackupData::class.java)
                val backupData = adapter.fromJson(json) ?: throw Exception("Data tidak valid")

                // Restore Alarms
                for (a in backupData.alarms) {
                    repository.insertAlarm(a)
                }

                // Restore main prefs
                val prefs = context.getSharedPreferences("alarm_grup_prefs", android.content.Context.MODE_PRIVATE).edit()
                for ((k, v) in backupData.stringPrefs) prefs.putString(k, v)
                for ((k, v) in backupData.booleanPrefs) prefs.putBoolean(k, v)
                for ((k, v) in backupData.intPrefs) prefs.putInt(k, v)
                prefs.apply()

                // Restore notes
                if (backupData.notesPrefs != null) {
                    context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putString("all_notes", backupData.notesPrefs).apply()
                }

                // Reload states
                _userName.value = backupData.stringPrefs["user_name"] ?: _userName.value
                val isGrp = backupData.booleanPrefs["is_offline_group"] ?: false
                _isOfflineGroup.value = isGrp
                _joinedGroupCode.value = backupData.stringPrefs["joined_group_code"]
                _joinedGroupName.value = backupData.stringPrefs["joined_group_name"] ?: ""
                
                launch(kotlinx.coroutines.Dispatchers.Main) { onResult(true, "Restore berhasil! Beberapa perubahan mungkin perlu restart aplikasi.") }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(kotlinx.coroutines.Dispatchers.Main) { onResult(false, e.message ?: "Gagal memulihkan backup") }
            }
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
