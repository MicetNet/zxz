package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceConfig
import com.example.data.model.AttendanceLog
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AdminHomeScreen(
    currentUser: User,
    authViewModel: AuthViewModel,
    attendanceViewModel: AttendanceViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Konfigurasi, 1 = Kelola User, 2 = Riwayat Global

    val config by attendanceViewModel.attendanceConfig.collectAsState()
    val allLogs by attendanceViewModel.allLogs.collectAsState()
    val isSyncing by attendanceViewModel.isSyncing.collectAsState()
    val syncStatus by attendanceViewModel.syncMessage.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Portal Administrator",
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Configs") },
                    label = { Text("Konfigurasi") },
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
                    icon = { Icon(Icons.Default.Person, contentDescription = "Users") },
                    label = { Text("Kelola User") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MidnightBg,
                        selectedTextColor = MintAccent,
                        indicatorColor = MintAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
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
            when (activeTab) {
                0 -> AdminConfigView(
                    config = config,
                    attendanceViewModel = attendanceViewModel,
                    isSyncing = isSyncing,
                    syncStatus = syncStatus
                )
                1 -> AdminUserListView(
                    authViewModel = authViewModel,
                    activeAdminId = currentUser.id
                )
                2 -> AdminGlobalLogsView(
                    allLogs = allLogs,
                    attendanceViewModel = attendanceViewModel
                )
            }
        }
    }
}

// ==========================================
// VIEW 1: CONFIGURATION TIME & WIFI & FIREBASE
// ==========================================
@Composable
fun AdminConfigView(
    config: AttendanceConfig,
    attendanceViewModel: AttendanceViewModel,
    isSyncing: Boolean,
    syncStatus: String
) {
    val context = LocalContext.current

    // Local variables mirroring model values on startup
    var wifiName by remember(config) { mutableStateOf(config.officeWifiName) }
    var macAddress by remember(config) { mutableStateOf(config.officeMacAddress) }
    var firebaseUrl by remember(config) { mutableStateOf(config.firebaseUrl) }

    var startTimeIn by remember(config) { mutableStateOf(config.startTimeIn) }
    var endTimeIn by remember(config) { mutableStateOf(config.endTimeIn) }
    var startBreak by remember(config) { mutableStateOf(config.startBreak) }
    var endBreak by remember(config) { mutableStateOf(config.endBreak) }
    var startOut by remember(config) { mutableStateOf(config.startOut) }
    var endOut by remember(config) { mutableStateOf(config.endOut) }
    var startOvertime by remember(config) { mutableStateOf(config.startOvertime) }
    var endOvertime by remember(config) { mutableStateOf(config.endOvertime) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firebase database setup card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = "Firebase Sync", tint = MintAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Integrasi Database Firebase", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }

                    Text("Masukkan alamat Firebase Realtime Database Anda di bawah ini agar log absensi dari seluruh karyawan tersimpan secara real-time.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    OutlinedTextField(
                        value = firebaseUrl,
                        onValueChange = { firebaseUrl = it },
                        modifier = Modifier.fillMaxWidth().testTag("firebase_url_input"),
                        label = { Text("Firebase URL") },
                        placeholder = { Text("https://PROJEK-ANDA.firebaseio.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintAccent,
                            unfocusedBorderColor = BorderDark
                        )
                    )

                    // Resilient trigger status
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MidnightBg),
                        border = BorderStroke(0.5.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status Antrian Sync:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(syncStatus, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MintSecondary)
                            }
                            Button(
                                onClick = { attendanceViewModel.triggerSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightCard),
                                border = BorderStroke(1.dp, MintAccent),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = MintAccent)
                                } else {
                                    Text("SYNC NOW", color = MintAccent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Office router WiFi config card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = "Router Rules", tint = MintAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parameter Router Wi-Fi Kantor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }

                    Text("Karyawan hanya bisa absen jika terhubung ke Wi-Fi dengan SSID dan BSSID (MAC Address Router) tepat berikut.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    OutlinedTextField(
                        value = wifiName,
                        onValueChange = { wifiName = it },
                        modifier = Modifier.fillMaxWidth().testTag("config_wifi_ssid"),
                        label = { Text("Nama Wi-Fi (SSID)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent, unfocusedBorderColor = BorderDark)
                    )

                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it.uppercase() },
                        modifier = Modifier.fillMaxWidth().testTag("config_wifi_mac"),
                        label = { Text("MAC Address Router (BSSID)") },
                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent, unfocusedBorderColor = BorderDark)
                    )
                }
            }
        }

        // Timing Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = "Time configs", tint = MintAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Waktu Kerja Karyawan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }

                    Text("Atur rentang jam (Format HH:mm) dimana masing-masing jenis absensi dinyatakan sah di sistem.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    // Grid or Columns for Masuk
                    TimeRangeFieldGroup("1. Masuk Kerja", startTimeIn, endTimeIn, {startTimeIn = it}, {endTimeIn = it})
                    TimeRangeFieldGroup("2. Istirahat Kerja", startBreak, endBreak, {startBreak = it}, {endBreak = it})
                    TimeRangeFieldGroup("3. Pulang Kerja", startOut, endOut, {startOut = it}, {endOut = it})
                    TimeRangeFieldGroup("4. Lembur Kerja", startOvertime, endOvertime, {startOvertime = it}, {endOvertime = it})
                }
            }
        }

        // Action controls
        item {
            Button(
                onClick = {
                    val updated = config.copy(
                        officeWifiName = wifiName,
                        officeMacAddress = macAddress,
                        firebaseUrl = firebaseUrl,
                        startTimeIn = startTimeIn,
                        endTimeIn = endTimeIn,
                        startBreak = startBreak,
                        endBreak = endBreak,
                        startOut = startOut,
                        endOut = endOut,
                        startOvertime = startOvertime,
                        endOvertime = endOvertime
                    )
                    attendanceViewModel.saveConfiguration(updated) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = MidnightBg),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_config_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SIMPAN SEMUA PERUBAHAN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        }
    }
}

@Composable
fun TimeRangeFieldGroup(
    title: String,
    start: String,
    end: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MintSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = start,
                onValueChange = onStartChange,
                modifier = Modifier.weight(1f),
                label = { Text("Mulai") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent, unfocusedBorderColor = BorderDark)
            )
            OutlinedTextField(
                value = end,
                onValueChange = onEndChange,
                modifier = Modifier.weight(1f),
                label = { Text("Selesai") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent, unfocusedBorderColor = BorderDark)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ==========================================
// VIEW 2: MANAGE USER CREDENTIALS (CRUD)
// ==========================================
@Composable
fun AdminUserListView(
    authViewModel: AuthViewModel,
    activeAdminId: Int
) {
    val context = LocalContext.current
    val users by authViewModel.allUsers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editUserTarget by remember { mutableStateOf<User?>(null) }

    // local inputs for insert/edit
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("EMPLOYEE") } // EMPLOYEE or ADMIN

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightSurface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kelola Karyawan & Admin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("${users.size} Pengguna Terdaftar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Button(
                    onClick = {
                        username = ""
                        password = ""
                        fullName = ""
                        role = "EMPLOYEE"
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = MidnightBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Add User", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users) { usr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(0.5.dp, BorderDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(usr.fullName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (usr.role == "ADMIN") Color(0x2E60A5FA) else Color(0x2E00FAAC))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = usr.role,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (usr.role == "ADMIN") Color(0xFF60A5FA) else MintAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Username : ${usr.username}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Password : ${usr.password}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    username = usr.username
                                    password = usr.password
                                    fullName = usr.fullName
                                    role = usr.role
                                    editUserTarget = usr
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MintAccent)
                            }

                            if (usr.id != activeAdminId) {
                                IconButton(
                                    onClick = {
                                        authViewModel.deleteUser(usr) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Insert user Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Pengguna Baru", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_user_full_name"),
                        label = { Text("Nama Lengkap") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_user_username"),
                        label = { Text("Username") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_user_password"),
                        label = { Text("Password") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent)
                    )
                    Column {
                        Text("Peran Akses (Role):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = role == "EMPLOYEE",
                                    onClick = { role = "EMPLOYEE" },
                                    colors = RadioButtonDefaults.colors(selectedColor = MintAccent)
                                )
                                Text("Employee", color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = role == "ADMIN",
                                    onClick = { role = "ADMIN" },
                                    colors = RadioButtonDefaults.colors(selectedColor = MintAccent)
                                )
                                Text("Admin", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.addUser(username, password, fullName, role) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = MidnightBg)
                ) {
                    Text("TAMBAH", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("BATAL", color = TextSecondary)
                }
            },
            containerColor = MidnightSurface
        )
    }

    // Edit user Dialog
    if (editUserTarget != null) {
        val target = editUserTarget!!
        AlertDialog(
            onDismissRequest = { editUserTarget = null },
            title = { Text("Edit Akun ${target.username}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_full_name"),
                        label = { Text("Nama Lengkap") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_password"),
                        label = { Text("Password") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintAccent)
                    )
                    Column {
                        Text("Role (Terkunci):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(role, color = MintSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(fullName = fullName, password = password)
                        authViewModel.updateUser(updated) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) editUserTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = MidnightBg)
                ) {
                    Text("SIMPAN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editUserTarget = null }) {
                    Text("BATAL", color = TextSecondary)
                }
            },
            containerColor = MidnightSurface
        )
    }
}

// ==========================================
// VIEW 3: GLOBAL LOG HISTORY TIMELINE (ALL EMPLOYEES)
// ==========================================
@Composable
fun AdminGlobalLogsView(
    allLogs: List<AttendanceLog>,
    attendanceViewModel: AttendanceViewModel
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(allLogs, searchQuery) {
        if (searchQuery.isBlank()) {
            allLogs
        } else {
            allLogs.filter { log ->
                log.fullName.contains(searchQuery, ignoreCase = true) ||
                log.username.contains(searchQuery, ignoreCase = true) ||
                log.type.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightSurface)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Semua Riwayat Absensi Karyawan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                
                // Finder box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("search_history_input"),
                    placeholder = { Text("Cari nama karyawan / jenis absen...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintAccent,
                        unfocusedBorderColor = BorderDark
                    )
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = "Not found", tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Data absensi tidak ditemukan.", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(filteredLogs) { log ->
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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.fullName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (log.type.uppercase()) {
                                                    "MASUK" -> Color(0x3100FAAC)
                                                    "ISTIRAHAT" -> Color(0x31FB923C)
                                                    "PULANG" -> Color(0x31F87171)
                                                    "LEMBUR" -> Color(0x31C084FC)
                                                    else -> Color(0x31F1F5F9)
                                                }
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.type,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = when (log.type.uppercase()) {
                                                "MASUK" -> MintAccent
                                                "ISTIRAHAT" -> Color(0xFFFB923C)
                                                "PULANG" -> Color(0xFFF87171)
                                                "LEMBUR" -> Color(0xFFC084FC)
                                                else -> Color.White
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Wifi: ${log.wifiName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("MAC: ${log.macAddress}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(log.time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text(log.date, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (log.synced) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = "Sync state",
                                        tint = if (log.synced) MintAccent else Color.Yellow,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (log.synced) "Synced Firebase" else "Lokal",
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
