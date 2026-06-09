package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceLog
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployeeHomeScreen(
    currentUser: User,
    attendanceViewModel: AttendanceViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Absen Sekarang, 1 = Riwayat Absen

    val config by attendanceViewModel.attendanceConfig.collectAsState()
    val userLogs by attendanceViewModel.userLogs.collectAsState()
    val currentWifi by attendanceViewModel.currentWifi.collectAsState()

    val simEnabled by attendanceViewModel.simulatedWifiEnabled.collectAsState()
    val simSsid by attendanceViewModel.simulatedSsid.collectAsState()
    val simBssid by attendanceViewModel.simulatedBssid.collectAsState()

    // Setup active logs filter for our user
    LaunchedEffect(currentUser) {
        attendanceViewModel.setLoggedUserId(currentUser.id)
    }

    // Dynamic digital clock
    var liveTime by remember { mutableStateOf("") }
    var liveDate by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            liveTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            liveDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    // Permission launcher for Location detection (for SSID)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            attendanceViewModel.refreshWifi()
        } else {
            Toast.makeText(context, "Izin Lokasi diperlukan untuk membaca Wi-Fi Kantor.", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-check on load
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            attendanceViewModel.refreshWifi()
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Portal Karyawan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MintAccent
                        )
                        Text(
                            text = currentUser.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Manual sync fallback button
                    IconButton(onClick = { attendanceViewModel.triggerSync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MintAccent)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MidnightSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Absen") },
                    label = { Text("Absen") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MidnightBg,
                        selectedTextColor = MintAccent,
                        indicatorColor = MintAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MidnightBg,
                        selectedTextColor = MintAccent,
                        indicatorColor = MintAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        },
        containerColor = MidnightBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeTab == 0) {
                // Absen Sekarang
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time Dynamic Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard),
                            shape = RoundedCornerShape(24.dp), // rounded-3xl
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = liveDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = liveTime,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Light, // font-light from design HTML
                                        letterSpacing = (-1).sp
                                    ),
                                    color = MintAccent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Device Secure",
                                        tint = MintSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Waktu Server Sinkron",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Wifi Connection Verification Card
                    item {
                        val activeSsid = if (simEnabled) simSsid else currentWifi.ssid
                        val activeBssid = if (simEnabled) simBssid else currentWifi.bssid
                        val wifiMatched = activeSsid.uppercase().trim() == config.officeWifiName.uppercase().trim() &&
                                activeBssid.uppercase().trim() == config.officeMacAddress.uppercase().trim()

                        val matchedBgColor = Color(0xFF1D2B1D) // exact color from design HTML (bg-[#1D2B1D])
                        val matchedTextCol = Color(0xFFB7F397) // exact color from design HTML (text-[#B7F397])

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (wifiMatched) matchedBgColor else Color(0xFF2B1014)
                            ),
                            shape = RoundedCornerShape(24.dp), // rounded-3xl matching theme
                            border = BorderStroke(1.dp, if (wifiMatched) matchedTextCol.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (wifiMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = "Wifi matched",
                                            tint = if (wifiMatched) matchedTextCol else ErrorRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Status Router Kantor",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp)) // rounded-full styled badge
                                            .background(if (wifiMatched) matchedBgColor else Color(0xFF4C1D1D))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (wifiMatched) "TERVERIFIKASI" else "TIDAK COCOK",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (wifiMatched) matchedTextCol else ErrorRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Active Wifi Display
                                Text(
                                    text = "Wi-Fi Terkoneksi : SSID = '$activeSsid'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "MAC Router      : BSSID = '$activeBssid'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = BorderDark, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Configured Target Wifi Display
                                Text(
                                    text = "Aturan Kantor : SSID = '${config.officeWifiName}'",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MintSecondary
                                )
                                Text(
                                    text = "Aturan Router : BSSID = '${config.officeMacAddress}'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                if (!simEnabled && currentWifi.errorMsg != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0x3DFFA000)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = currentWifi.errorMsg ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Simulated WiFi Controls (Emulator/Demo Friendly)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard),
                            shape = RoundedCornerShape(24.dp), // rounded-3xl matching theme
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Simulasi Wi-Fi (Sangat berguna di Emulator)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Gunakan ini untuk melewati pembatasan hardware pada sandbox",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = simEnabled,
                                        onCheckedChange = { attendanceViewModel.toggleSimulation(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MintAccent,
                                            checkedTrackColor = Color(0xFF144D3A)
                                        )
                                    )
                                }

                                if (simEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                attendanceViewModel.updateSimulatedWifi(
                                                    config.officeWifiName,
                                                    config.officeMacAddress
                                                )
                                                Toast.makeText(context, "Mencocokkan Wi-Fi Kantor!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A24)),
                                            border = BorderStroke(1.dp, MintSecondary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set Wifi Kantor", color = MintAccent, style = MaterialTheme.typography.labelSmall)
                                        }

                                        Button(
                                            onClick = {
                                                attendanceViewModel.updateSimulatedWifi(
                                                    "Bukan_Wifi_Kantor",
                                                    "AA:BB:CC:DD:EE:FF"
                                                )
                                                Toast.makeText(context, "Mencocokkan Wi-Fi Salah!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331418)),
                                            border = BorderStroke(1.dp, ErrorRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set Wifi Salah", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Attendance Buttons Title
                    item {
                        Text(
                            text = "Aksi Absensi Harian",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Attendance List - strictly parsed to avoid custom loop deconstruction issues
                    item {
                        val todayDateStr = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val typesList = listOf("MASUK", "ISTIRAHAT", "PULANG", "LEMBUR")
                            typesList.forEach { typeCode ->
                                val titleLabel = when (typeCode) {
                                    "MASUK" -> "Absen Masuk"
                                    "ISTIRAHAT" -> "Absen Istirahat"
                                    "PULANG" -> "Absen Pulang"
                                    else -> "Absen Lembur"
                                }
                                val iconData = when (typeCode) {
                                    "MASUK" -> Icons.Default.PlayArrow
                                    "ISTIRAHAT" -> Icons.Default.Build
                                    "PULANG" -> Icons.Default.Home
                                    else -> Icons.Default.Star
                                }
                                val timingLabel = when (typeCode) {
                                    "MASUK" -> "${config.startTimeIn} - ${config.endTimeIn}"
                                    "ISTIRAHAT" -> "${config.startBreak} - ${config.endBreak}"
                                    "PULANG" -> "${config.startOut} - ${config.endOut}"
                                    else -> "${config.startOvertime} - ${config.endOvertime}"
                                }

                                val hasLoggedLog = userLogs.firstOrNull { it.type.uppercase() == typeCode.uppercase() && it.date == todayDateStr }

                                AttendanceActionRow(
                                    title = titleLabel,
                                    timeRange = timingLabel,
                                    icon = iconData,
                                    alreadyLogged = hasLoggedLog != null,
                                    loggedTime = hasLoggedLog?.time,
                                    isSynced = hasLoggedLog?.synced ?: false,
                                    onClick = {
                                        attendanceViewModel.performAttendance(currentUser, typeCode) { _, message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab Riwayat Absen
                TabHistoryView(userLogs = userLogs)
            }
        }
    }
}

@Composable
fun AttendanceActionRow(
    title: String,
    timeRange: String,
    icon: ImageVector,
    alreadyLogged: Boolean,
    loggedTime: String?,
    isSynced: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_button_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (alreadyLogged) Color(0xFF141D2B) else MidnightCard
        ),
        border = BorderStroke(
            1.dp,
            if (alreadyLogged) Color(0xFF334155) else BorderDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (alreadyLogged) Color(0xFF1E293B) else Color(0xFF144D3A))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (alreadyLogged) TextSecondary else MintAccent
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (alreadyLogged) TextSecondary else Color.White
                    )
                    Text(
                        text = "Rentang: $timeRange Waktu",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (alreadyLogged) {
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F1A2C))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sudah Absen",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF60A5FA)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = loggedTime ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isSynced) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = if (isSynced) "Synced to Firebase" else "Local Only",
                            tint = if (isSynced) MintAccent else Color.Yellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintAccent,
                        contentColor = MidnightBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ABSEN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun TabHistoryView(
    userLogs: List<AttendanceLog>
) {
    if (userLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Empty History",
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Riwayat Absensi Kosong",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Belum ada absensi terdaftar untuk Anda hari ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timeline Riwayat Absen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${userLogs.size} Catatan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MintSecondary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(userLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(0.5.dp, BorderDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (colorType, typeIcon) = when (log.type.uppercase()) {
                                    "MASUK" -> Pair(MintAccent, Icons.Default.PlayArrow)
                                    "ISTIRAHAT" -> Pair(Color(0xFFFB923C), Icons.Default.Build)
                                    "PULANG" -> Pair(Color(0xFFF87171), Icons.Default.Home)
                                    "LEMBUR" -> Pair(Color(0xFFC084FC), Icons.Default.Star)
                                    else -> Pair(Color.White, Icons.Default.Check)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorType.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = log.type,
                                        tint = colorType,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Absen ${log.type}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wifi: ${log.wifiName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "MAC: ${log.macAddress}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = log.time,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = log.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (log.synced) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = if (log.synced) "Synced" else "Offline",
                                        tint = if (log.synced) MintAccent else Color.Yellow,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (log.synced) "Firebase" else "Lokal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (log.synced) MintAccent else Color.Yellow
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
