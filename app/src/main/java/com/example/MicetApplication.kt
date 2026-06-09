package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.UserRepository
import com.example.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MicetApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val userRepository by lazy { UserRepository(database.userDao()) }
    val attendanceRepository by lazy { AttendanceRepository(database.attendanceLogDao(), database.attendanceConfigDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels
        NotificationHelper.createNotificationChannel(this)
    }
}
