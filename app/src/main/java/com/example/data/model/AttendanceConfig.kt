package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_configs")
data class AttendanceConfig(
    @PrimaryKey val id: Int = 1,
    val officeWifiName: String = "Micet.Net_Office",
    val officeMacAddress: String = "00:11:22:33:44:55",
    val startTimeIn: String = "07:00",
    val endTimeIn: String = "09:00",
    val startBreak: String = "11:30",
    val endBreak: String = "13:00",
    val startOut: String = "16:30",
    val endOut: String = "18:00",
    val startOvertime: String = "18:01",
    val endOvertime: String = "22:00",
    val firebaseUrl: String = "https://micet-net-default-rtdb.firebaseio.com",
    val firebaseApiKey: String = ""
)
