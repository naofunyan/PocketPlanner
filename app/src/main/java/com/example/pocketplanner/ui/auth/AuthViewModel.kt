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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.pocketplanner.R

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
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
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: context.getString(R.string.auth_error_login_failed))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: context.getString(R.string.auth_error_google_failed))
            }
        }
    }

    fun register(username: String, email: String, pass: String) {
        if (!username.matches(Regex("^[a-zA-Z0-9]+$"))) {
            _uiState.value = AuthState.Error(context.getString(R.string.auth_error_username_invalid))
            return
        }
        if (pass.length < 8 || !pass.matches(Regex(".*\\d.*"))) {
            _uiState.value = AuthState.Error(context.getString(R.string.auth_error_password_invalid))
            return
        }
        if (email.isBlank()) {
            _uiState.value = AuthState.Error(context.getString(R.string.auth_error_email_empty))
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
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: context.getString(R.string.auth_error_registration_failed))
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, context.getString(R.string.auth_error_reset_email_empty))
            return
        }

        com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, context.getString(R.string.auth_success_reset_email_sent))
                } else {
                    onResult(false, task.exception?.message ?: context.getString(R.string.auth_error_reset_failed))
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