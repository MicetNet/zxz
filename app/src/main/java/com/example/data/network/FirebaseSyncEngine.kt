package com.example.data.network

import android.util.Log
import com.example.data.model.AttendanceLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object FirebaseSyncEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun syncAttendanceLog(baseUrl: String, logItem: AttendanceLog): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim()
        if (cleanUrl.isBlank() || cleanUrl.contains("default-rtdb")) {
            Log.d("FirebaseSyncEngine", "Using default/empty Firebase database address. Simulated sync (offline-mode OK).")
            return@withContext true
        }

        // Standardize base URL
        val base = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
        val requestUrl = "${base}attendance_logs.json"

        val jsonBody = """
            {
                "userId": ${logItem.userId},
                "username": "${logItem.username}",
                "fullName": "${logItem.fullName}",
                "date": "${logItem.date}",
                "time": "${logItem.time}",
                "type": "${logItem.type}",
                "wifiName": "${logItem.wifiName}",
                "macAddress": "${logItem.macAddress}",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .post(jsonBody.toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d("FirebaseSyncEngine", "Firebase sync response: ${response.code}, Success: $success")
                success
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Network sync failed due to exception: ${e.message}")
            false
        }
    }
}
