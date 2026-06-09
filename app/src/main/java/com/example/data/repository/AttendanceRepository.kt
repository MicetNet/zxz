package com.example.data.repository

import android.util.Log
import com.example.data.database.AttendanceConfigDao
import com.example.data.database.AttendanceLogDao
import com.example.data.model.AttendanceConfig
import com.example.data.model.AttendanceLog
import com.example.data.network.FirebaseSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AttendanceRepository(
    private val logDao: AttendanceLogDao,
    private val configDao: AttendanceConfigDao
) {
    val allLogs: Flow<List<AttendanceLog>> = logDao.getAllLogsFlow()

    fun getLogsByUserId(userId: Int): Flow<List<AttendanceLog>> {
        return logDao.getLogsByUserIdFlow(userId)
    }

    suspend fun getLogsByUserAndDate(userId: Int, date: String): List<AttendanceLog> {
        return logDao.getLogsByUserAndDate(userId, date)
    }

    val liveConfig: Flow<AttendanceConfig> = configDao.getConfigFlow().map { it ?: AttendanceConfig() }

    suspend fun getConfigDirect(): AttendanceConfig {
        return configDao.getConfigDirect() ?: AttendanceConfig()
    }

    suspend fun saveConfig(config: AttendanceConfig) {
        configDao.insertConfig(config)
    }

    suspend fun insertLog(log: AttendanceLog): Boolean {
        // 1. Insert locally first
        val id = logDao.insertLog(log)
        val insertedLog = log.copy(id = id.toInt())

        // 2. Fetch direct config (to check Firebase URL)
        val currentConfig = getConfigDirect()
        
        // 3. Attempt to Sync to Firebase
        val isSynced = FirebaseSyncEngine.syncAttendanceLog(currentConfig.firebaseUrl, insertedLog)
        
        if (isSynced) {
            // Update to synced in local database
            logDao.updateLog(insertedLog.copy(synced = true))
        }
        
        return isSynced
    }

    suspend fun deleteLog(log: AttendanceLog) {
        logDao.deleteLog(log)
    }

    suspend fun trySyncUnsyncedLogs(): Int = withContext(Dispatchers.IO) {
        val currentConfig = getConfigDirect()
        val unsyncedList = logDao.getUnsyncedLogs()
        var successCount = 0
        Log.d("AttendanceRepository", "Found ${unsyncedList.size} unsynced logs.")
        
        for (log in unsyncedList) {
            val isSynced = FirebaseSyncEngine.syncAttendanceLog(currentConfig.firebaseUrl, log)
            if (isSynced) {
                logDao.updateLog(log.copy(synced = true))
                successCount++
            }
        }
        successCount
    }
}
