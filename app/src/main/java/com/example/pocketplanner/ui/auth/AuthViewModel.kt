package com.example.pocketplanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }

    fun register(username: String, email: String, pass: String, confirmPass: String) {
        if (!username.matches(Regex("^[a-zA-Z0-9]+$"))) {
            _uiState.value = AuthState.Error("Username must be alphanumeric.")
            return
        }
        if (pass.length < 8 || !pass.matches(Regex(".*\\d.*"))) {
            _uiState.value = AuthState.Error("Password must be at least 8 characters and contain a number.")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = AuthState.Error("Passwords do not match.")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AuthState.Error("Email cannot be empty.")
            return
        }

        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    user?.updateProfile(profileUpdates)?.await()
                } catch (e: Exception) {
                    // Ignore profile update errors for now, the user was still created
                }
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration Failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}