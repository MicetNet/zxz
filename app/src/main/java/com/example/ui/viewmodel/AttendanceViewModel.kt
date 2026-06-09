package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceConfig
import com.example.data.model.AttendanceLog
import com.example.data.model.User
import com.example.data.repository.AttendanceRepository
import com.example.utils.NotificationHelper
import com.example.utils.WifiDetails
import com.example.utils.WifiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val context: Context
) : ViewModel() {

    private val _currentWifi = MutableStateFlow(WifiDetails("", "", false))
    val currentWifi: StateFlow<WifiDetails> = _currentWifi.asStateFlow()

    // Simulation settings for testing on emulators
    private val _simulatedWifiEnabled = MutableStateFlow(true) // Default to true so it is easy to test on preview emulators!
    val simulatedWifiEnabled: StateFlow<Boolean> = _simulatedWifiEnabled.asStateFlow()

    private val _simulatedSsid = MutableStateFlow("Micet.Net_Office")
    val simulatedSsid: StateFlow<String> = _simulatedSsid.asStateFlow()

    private val _simulatedBssid = MutableStateFlow("00:11:22:33:44:55")
    val simulatedBssid: StateFlow<String> = _simulatedBssid.asStateFlow()

    val attendanceConfig: StateFlow<AttendanceConfig> = attendanceRepository.liveConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttendanceConfig()
        )

    val allLogs: StateFlow<List<AttendanceLog>> = attendanceRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedUserId = MutableStateFlow<Int?>(null)
    
    val userLogs: StateFlow<List<AttendanceLog>> = combine(allLogs, _selectedUserId) { logs, userId ->
        if (userId == null) emptyList() else logs.filter { it.userId == userId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _syncMessage = MutableStateFlow<String>("Sistem Berjalan")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        refreshWifi()
    }

    fun setLoggedUserId(userId: Int?) {
        _selectedUserId.value = userId
    }

    fun refreshWifi() {
        viewModelScope.launch {
            try {
                val details = WifiHelper.getCurrentWifiDetails(context)
                _currentWifi.value = details
                
                // If we aren't simulating, make sure BSSID/SSID are updated
                if (!_simulatedWifiEnabled.value) {
                    _simulatedSsid.value = details.ssid
                    _simulatedBssid.value = details.bssid
                }
            } catch (e: Exception) {
                _currentWifi.value = WifiDetails("", "", false, e.message)
            }
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        _simulatedWifiEnabled.value = enabled
        if (enabled) {
            // Apply default set office values if fields are empty
            viewModelScope.launch {
                val conf = attendanceRepository.getConfigDirect()
                _simulatedSsid.value = conf.officeWifiName
                _simulatedBssid.value = conf.officeMacAddress
            }
        } else {
            refreshWifi()
        }
    }

    fun updateSimulatedWifi(ssid: String, bssid: String) {
        _simulatedSsid.value = ssid.trim()
        _simulatedBssid.value = bssid.trim().uppercase()
    }

    fun saveConfiguration(config: AttendanceConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                attendanceRepository.saveConfig(config)
                onResult(true, "Konfigurasi absen berhasil disimpan.")
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan konfigurasi: ${e.message}")
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Menghubungi Firebase..."
            try {
                val count = attendanceRepository.trySyncUnsyncedLogs()
                _syncMessage.value = "Sinkronisasi Berhasil: $count data terkirim ke Firebase."
            } catch (e: Exception) {
                _syncMessage.value = "Gagal Sinkronisasi: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun performAttendance(user: User, type: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val dateNow = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
            val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val hhmmNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val config = attendanceRepository.getConfigDirect()

            // 1. Validate Wi-Fi credentials
            val (currentSsid, currentBssid) = if (_simulatedWifiEnabled.value) {
                Pair(_simulatedSsid.value, _simulatedBssid.value)
            } else {
                refreshWifi()
                Pair(_currentWifi.value.ssid, _currentWifi.value.bssid)
            }

            if (currentSsid.uppercase() != config.officeWifiName.uppercase() ||
                currentBssid.uppercase() != config.officeMacAddress.uppercase()
            ) {
                onResult(
                    false,
                    "Gagal Absen: Anda harus terhubung ke Wi-Fi Kantor (${config.officeWifiName}) " +
                            "dengan MAC MAC (${config.officeMacAddress}). Wi-Fi Terdeteksi: SSID = '$currentSsid', MAC = '$currentBssid'"
                )
                return@launch
            }

            // 2. Validate Click Limit: "absen hanya bisa 1 kali klik tiap hari"
            val existingLogs = attendanceRepository.getLogsByUserAndDate(user.id, dateNow)
            val alreadyAttendedThisType = existingLogs.any { it.type.uppercase() == type.uppercase() }
            if (alreadyAttendedThisType) {
                onResult(false, "Gagal Absen: Anda sudah melakukan Absen $type hari ini.")
                return@launch
            }

            // 3. Validate Session Timings
            val (startTime, endTime) = when (type.uppercase()) {
                "MASUK" -> Pair(config.startTimeIn, config.endTimeIn)
                "ISTIRAHAT" -> Pair(config.startBreak, config.endBreak)
                "PULANG" -> Pair(config.startOut, config.endOut)
                "LEMBUR" -> Pair(config.startOvertime, config.endOvertime)
                else -> Pair("00:00", "23:59")
            }

            if (!isTimeInRange(hhmmNow, startTime, endTime)) {
                onResult(
                    false,
                    "Peringatan: Diluar rentang jam $type ($startTime - $endTime). Anda absen saat ini pukul $hhmmNow."
                )
                // If you want hard stop, uncomment "return@launch". Let's block it for hard security boundaries, conforming to "absen hanya bisa jam masuk..."!
                return@launch
            }

            // Create log
            val logItem = AttendanceLog(
                userId = user.id,
                username = user.username,
                fullName = user.fullName,
                date = dateNow,
                time = timeNow,
                type = type.uppercase(),
                wifiName = currentSsid,
                macAddress = currentBssid,
                synced = false
            )

            val syncSuccessful = attendanceRepository.insertLog(logItem)

            // Trigger Push Notification immediately
            NotificationHelper.sendAttendanceSuccessNotification(
                context = context,
                employeeName = user.fullName,
                attendanceType = type,
                timeString = timeNow
            )

            if (syncSuccessful) {
                onResult(true, "Absen $type Berhasil! Tersimpan & realtime sinkron dengan database Firebase.")
            } else {
                onResult(true, "Absen $type Berhasil dicatat lokal offline (Belum sinkron Firebase).")
            }
        }
    }

    private fun isTimeInRange(current: String, start: String, end: String): Boolean {
        return try {
            val currMin = parseTimeToMinutes(current)
            val startMin = parseTimeToMinutes(start)
            val endMin = parseTimeToMinutes(end)

            if (startMin <= endMin) {
                currMin in startMin..endMin
            } else {
                // cross midnight
                currMin >= startMin || currMin <= endMin
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size >= 2) {
            val hrs = parts[0].toIntOrNull() ?: 0
            val mins = parts[1].toIntOrNull() ?: 0
            return hrs * 60 + mins
        }
        return 0
    }

    class Factory(
        private val attendanceRepository: AttendanceRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
                return AttendanceViewModel(attendanceRepository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
