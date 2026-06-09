package com.example.data.database

import androidx.room.*
import com.example.data.model.AttendanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM attendance_logs ORDER BY date DESC, time DESC")
    fun getAllLogsFlow(): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getLogsByUserIdFlow(userId: Int): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE userId = :userId AND date = :date")
    suspend fun getLogsByUserAndDate(userId: Int, date: String): List<AttendanceLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLog): Long

    @Update
    suspend fun updateLog(log: AttendanceLog)

    @Delete
    suspend fun deleteLog(log: AttendanceLog)

    @Query("SELECT * FROM attendance_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<AttendanceLog>
}
