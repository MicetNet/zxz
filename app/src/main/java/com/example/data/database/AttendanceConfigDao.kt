package com.example.data.database

import androidx.room.*
import com.example.data.model.AttendanceConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceConfigDao {
    @Query("SELECT * FROM attendance_configs WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AttendanceConfig?>

    @Query("SELECT * FROM attendance_configs WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): AttendanceConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AttendanceConfig)
}
