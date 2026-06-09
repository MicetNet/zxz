package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "micet_net_notifications"
    private const val CHANNEL_NAME = "Absensi Micet.Net"
    private const val CHANNEL_DESC = "Notifikasi status absensi karyawan Micet.Net"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendAttendanceSuccessNotification(
        context: Context,
        employeeName: String,
        attendanceType: String,
        timeString: String
    ) {
        val typeLabel = when (attendanceType.uppercase()) {
            "MASUK" -> "Masuk Kerja"
            "ISTIRAHAT" -> "Mulai Istirahat"
            "PULANG" -> "Pulang Kerja"
            "LEMBUR" -> "Lembur Kerja"
            else -> attendanceType
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard system info icon
            .setContentTitle("Absensi Berhasil! \uD83D\uDCDD")
            .setContentText("Absen $typeLabel untuk $employeeName pada pukul $timeString berhasil dicatat.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Note: If running on Android 13+ (API 33), permission is checked in UI.
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
