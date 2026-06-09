package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

data class WifiDetails(
    val ssid: String,
    val bssid: String,
    val isConnected: Boolean,
    val errorMsg: String? = null
)

object WifiHelper {
    fun getCurrentWifiDetails(context: Context): WifiDetails {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return WifiDetails("", "", false, "Gagal mengakses layanan jaringan")
            
        val activeNetwork = connectivityManager.activeNetwork
            ?: return WifiDetails("", "", false, "Terputus dari internet/jaringan")
            
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return WifiDetails("", "", false, "Informasi jaringan tidak tersedia")

        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        if (!hasWifi) {
            return WifiDetails("", "", false, "Gunakan Wi-Fi Kantor, bukan koneksi data seluler.")
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return WifiDetails("", "", false, "Layanan Wi-Fi tidak aktif")
            
        @Suppress("DEPRECATION")
        val connectionInfo: WifiInfo? = wifiManager.connectionInfo

        if (connectionInfo == null) {
            return WifiDetails("", "", false, "Info koneksi Wi-Fi tidak ditemukan")
        }

        var ssid = connectionInfo.ssid ?: ""
        var bssid = connectionInfo.bssid ?: ""

        // Standardize outer quotes removal
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            if (ssid.length > 2) {
                ssid = ssid.substring(1, ssid.length - 1)
            }
        }

        // If location is disabled, Android returns <unknown ssid> & 02:00:00:00:00:00
        if (ssid == "<unknown ssid>" || bssid == "02:00:00:00:00:00" || bssid.isBlank()) {
            return WifiDetails(
                ssid = ssid,
                bssid = bssid,
                isConnected = true,
                errorMsg = "Izin Lokasi (GPS) harus aktif untuk mendeteksi Nama Wi-Fi & MAC Router."
            )
        }

        return WifiDetails(
            ssid = ssid,
            bssid = bssid.uppercase(),
            isConnected = true
        )
    }
}
