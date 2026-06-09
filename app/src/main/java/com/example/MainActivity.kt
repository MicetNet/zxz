package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AdminHomeScreen
import com.example.ui.screens.EmployeeHomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory((application as MicetApplication).userRepository)
    }

    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AttendanceViewModel.Factory(
            (application as MicetApplication).attendanceRepository,
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("LOGIN") }
                    val currentUser by authViewModel.currentUser.collectAsState()

                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            "LOGIN" -> LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = { role, _ ->
                                    if (role == "ADMIN") {
                                        currentScreen = "ADMIN"
                                    } else {
                                        currentScreen = "EMPLOYEE"
                                    }
                                }
                            )

                            "EMPLOYEE" -> {
                                val user = currentUser
                                if (user != null) {
                                    EmployeeHomeScreen(
                                        currentUser = user,
                                        attendanceViewModel = attendanceViewModel,
                                        onLogout = {
                                            authViewModel.logout()
                                            attendanceViewModel.setLoggedUserId(null)
                                            currentScreen = "LOGIN"
                                        }
                                    )
                                } else {
                                    currentScreen = "LOGIN"
                                }
                            }

                            "ADMIN" -> {
                                val user = currentUser
                                if (user != null) {
                                    AdminHomeScreen(
                                        currentUser = user,
                                        authViewModel = authViewModel,
                                        attendanceViewModel = attendanceViewModel,
                                        onLogout = {
                                            authViewModel.logout()
                                            attendanceViewModel.setLoggedUserId(null)
                                            currentScreen = "LOGIN"
                                        }
                                    )
                                } else {
                                    currentScreen = "LOGIN"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
