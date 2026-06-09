package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val allUsers: StateFlow<List<User>> = userRepository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun login(usernameInput: String, passwordInput: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            val uName = usernameInput.trim()
            val pWord = passwordInput.trim()

            if (uName.isBlank() || pWord.isBlank()) {
                _loginError.value = "Username dan password tidak boleh kosong."
                onResult(false)
                return@launch
            }

            val user = userRepository.getUserByUsername(uName)
            if (user == null || user.password != pWord) {
                _loginError.value = "Username atau password salah."
                onResult(false)
            } else {
                _currentUser.value = user
                onResult(true)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
    }

    fun addUser(usernameInput: String, passwordInput: String, fullNameInput: String, roleInput: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uName = usernameInput.trim()
            val pWord = passwordInput.trim()
            val name = fullNameInput.trim()

            if (uName.isBlank() || pWord.isBlank() || name.isBlank()) {
                onResult(false, "Lengkapi semua data input.")
                return@launch
            }

            // Check duplicate
            val existing = userRepository.getUserByUsername(uName)
            if (existing != null) {
                onResult(false, "Username '$uName' sudah digunakan.")
                return@launch
            }

            val user = User(username = uName, password = pWord, fullName = name, role = roleInput)
            userRepository.insertUser(user)
            onResult(true, "Berhasil menambahkan $roleInput.")
        }
    }

    fun updateUser(user: User, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (user.username.isBlank() || user.password.isBlank() || user.fullName.isBlank()) {
                onResult(false, "Lengkapi semua data input.")
                return@launch
            }
            userRepository.updateUser(user)
            // If we modified ourselves
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
            onResult(true, "Data berhasil diperbarui.")
        }
    }

    fun deleteUser(user: User, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (_currentUser.value?.id == user.id) {
                onResult(false, "Tidak bisa menghapus akun Anda sendiri yang sedang aktif.")
                return@launch
            }
            userRepository.deleteUser(user)
            onResult(true, "Username '${user.username}' berhasil dihapus.")
        }
    }

    class Factory(private val repository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
