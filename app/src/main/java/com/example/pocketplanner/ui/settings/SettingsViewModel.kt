package com.example.pocketplanner.ui.settings

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val dataStore: DataStore<Preferences> // <-- INJECTED DATASTORE
    // TODO: Inject your Room Database Repositories/DAOs here in the future to clear local data upon deletion
) : ViewModel() {

    // --- DATASTORE KEYS ---
    companion object {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FALL_DETECTION = booleanPreferencesKey("fall_detection")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val SEND_SOS = booleanPreferencesKey("send_sos")
        val EMERGENCY_NUMBER = stringPreferencesKey("emergency_number")
        val ATTACH_PICTURES = booleanPreferencesKey("attach_pictures")
        val ATTACH_AUDIO = booleanPreferencesKey("attach_audio")
        val EMERGENCY_CONTACTS = stringSetPreferencesKey("emergency_contacts")
    }

    // --- PERSISTENT STATES ---
    val themeMode = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    val fallDetectionEnabled = dataStore.data.map { it[FALL_DETECTION] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val notificationsEnabled = dataStore.data.map { it[NOTIFICATIONS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // --- UPDATE FUNCTIONS ---
    fun updateThemeMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_MODE] = mode }
            AppCompatDelegate.setDefaultNightMode(mode) // Apply instantly
        }
    }

    fun updateFallDetection(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[FALL_DETECTION] = enabled }
    }

    fun updateNotifications(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    // --- FIREBASE AVATAR UPLOAD LOGIC ---
    private val _avatarUrl = MutableStateFlow(auth.currentUser?.photoUrl?.toString())
    val avatarUrl = _avatarUrl.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun uploadAvatar(uri: Uri) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isUploading.value = true
            _errorMessage.value = null
            try {
                val avatarRef = storage.reference.child("avatars/${user.uid}.jpg")
                avatarRef.putFile(uri).await()
                val downloadUrl = avatarRef.downloadUrl.await()

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUrl)
                    .build()
                user.updateProfile(profileUpdates).await()

                _avatarUrl.value = downloadUrl.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage ?: "Failed to upload image. Please try again."
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- ACCOUNT DELETION LOGIC ---
    fun deleteAccount(onSuccess: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "No user logged in."
            return
        }

        viewModelScope.launch {
            _isUploading.value = true // Reuse the uploading state to show the loading spinner during deletion
            _errorMessage.value = null
            try {
                // 1. Delete user's avatar from Cloud Storage
                try {
                    storage.reference.child("avatars/${user.uid}.jpg").delete().await()
                } catch (e: Exception) {
                    // Ignore if file doesn't exist or was already deleted
                }

                // TODO: 2. Clear local Room Database here!
                // e.g., tripDao.clearAll(), expenseDao.clearAll()

                // 3. Clear DataStore preferences
                dataStore.edit { it.clear() }

                // 4. Finally, delete the Firebase Auth User
                user.delete().await()

                _isUploading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isUploading.value = false
                e.printStackTrace()
                // Firebase Security Rule: A user must have logged in recently to delete their account
                _errorMessage.value = "Security Error: You must log out and log back in before deleting your account."
            }
        }
    }

    // 1. Add the key inside your companion object
    val HIGH_SENSITIVITY = booleanPreferencesKey("high_sensitivity")

    // 2. Add the state flow below your fallDetectionEnabled variable
    val highSensitivityEnabled = dataStore.data.map { it[HIGH_SENSITIVITY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 3. Add the update function
    fun updateHighSensitivity(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[HIGH_SENSITIVITY] = enabled }
    }

    // 1. Add the state flow below your highSensitivityEnabled variable
    val sendSosEnabled = dataStore.data.map { it[SEND_SOS] ?: true } // Defaulting to true for safety
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 2. Add the update function
    fun updateSendSos(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[SEND_SOS] = enabled }
    }

    // 2. Add the state flow below your other flows
    val emergencyNumber = dataStore.data.map { it[EMERGENCY_NUMBER] ?: "113" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "113")

    // 3. Add the update function
    fun updateEmergencyNumber(number: String) = viewModelScope.launch {
        dataStore.edit { it[EMERGENCY_NUMBER] = number }
    }

    // 2. Add the state flows
    val attachPicturesEnabled = dataStore.data.map { it[ATTACH_PICTURES] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val attachAudioEnabled = dataStore.data.map { it[ATTACH_AUDIO] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 3. Add the update functions
    fun updateAttachPictures(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[ATTACH_PICTURES] = enabled }
    }

    fun updateAttachAudio(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[ATTACH_AUDIO] = enabled }
    }

    // 2. Add the state flow
    val emergencyContacts = dataStore.data.map { it[EMERGENCY_CONTACTS] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // 3. Add the update function to append a new contact
    fun addEmergencyContact(contactData: String) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val currentContacts = prefs[EMERGENCY_CONTACTS] ?: emptySet()
            prefs[EMERGENCY_CONTACTS] = currentContacts + contactData
        }
    }

    fun removeEmergencyContact(contactData: String) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val currentContacts = prefs[EMERGENCY_CONTACTS] ?: emptySet()
            // Create a new set without the removed contact
            prefs[EMERGENCY_CONTACTS] = currentContacts - contactData
        }
    }
}