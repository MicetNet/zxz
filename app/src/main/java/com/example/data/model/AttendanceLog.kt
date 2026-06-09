package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val username: String,
    val fullName: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm:ss
    val type: String, // "MASUK", "ISTIRAHAT", "PULANG", "LEMBUR"
    val wifiName: String,
    val macAddress: String,
    val synced: Boolean = false
)
