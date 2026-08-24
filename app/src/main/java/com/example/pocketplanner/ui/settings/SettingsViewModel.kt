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
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // --- DATASTORE KEYS ---
    companion object {
        // App Preferences
        val THEME_MODE = intPreferencesKey("theme_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")

        // Fall Detection & Safety
        val FALL_DETECTION = booleanPreferencesKey("fall_detection")
        val HIGH_SENSITIVITY = booleanPreferencesKey("high_sensitivity")
        val SEND_SOS = booleanPreferencesKey("send_sos")
        val EMERGENCY_NUMBER = stringPreferencesKey("emergency_number")
        val ATTACH_PICTURES = booleanPreferencesKey("attach_pictures")
        val ATTACH_AUDIO = booleanPreferencesKey("attach_audio")
        val EMERGENCY_CONTACTS = stringSetPreferencesKey("emergency_contacts")

        // Medical Info
        val MEDICAL_NAME = stringPreferencesKey("medical_name")
        val MEDICAL_CONDITIONS = stringPreferencesKey("medical_conditions")
        val MEDICAL_BLOOD_TYPE = stringPreferencesKey("medical_blood_type")
        val MEDICAL_ALLERGIES = stringPreferencesKey("medical_allergies")
        val MEDICAL_MEDICATIONS = stringPreferencesKey("medical_medications")
        val MEDICAL_WEIGHT = stringPreferencesKey("medical_weight")
        val MEDICAL_HEIGHT = stringPreferencesKey("medical_height")
        val MEDICAL_DOB = stringPreferencesKey("medical_dob")
        val MEDICAL_ADDRESS = stringPreferencesKey("medical_address")
        val MEDICAL_ORGAN_DONOR = stringPreferencesKey("medical_organ_donor")
        val MEDICAL_NOTES = stringPreferencesKey("medical_notes")
        val SHARE_DURING_EMERGENCY = booleanPreferencesKey("share_during_emergency")
    }

    // --- PERSISTENT STATES (DATASTORE) ---

    // App Preferences
    val themeMode = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    val notificationsEnabled = dataStore.data.map { it[NOTIFICATIONS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Fall Detection & Safety
    val fallDetectionEnabled = dataStore.data.map { it[FALL_DETECTION] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val highSensitivityEnabled = dataStore.data.map { it[HIGH_SENSITIVITY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sendSosEnabled = dataStore.data.map { it[SEND_SOS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val emergencyNumber = dataStore.data.map { it[EMERGENCY_NUMBER] ?: "113" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "113")

    val attachPicturesEnabled = dataStore.data.map { it[ATTACH_PICTURES] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val attachAudioEnabled = dataStore.data.map { it[ATTACH_AUDIO] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val emergencyContacts = dataStore.data.map { it[EMERGENCY_CONTACTS] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // Medical Info
    val medicalName = dataStore.data.map { it[MEDICAL_NAME] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalConditions = dataStore.data.map { it[MEDICAL_CONDITIONS] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalBloodType = dataStore.data.map { it[MEDICAL_BLOOD_TYPE] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalAllergies = dataStore.data.map { it[MEDICAL_ALLERGIES] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalMedications = dataStore.data.map { it[MEDICAL_MEDICATIONS] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalWeight = dataStore.data.map { it[MEDICAL_WEIGHT] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalHeight = dataStore.data.map { it[MEDICAL_HEIGHT] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalDob = dataStore.data.map { it[MEDICAL_DOB] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalAddress = dataStore.data.map { it[MEDICAL_ADDRESS] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalOrganDonor = dataStore.data.map { it[MEDICAL_ORGAN_DONOR] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalNotes = dataStore.data.map { it[MEDICAL_NOTES] ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val shareDuringEmergencyEnabled = dataStore.data.map { it[SHARE_DURING_EMERGENCY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- DATASTORE UPDATE FUNCTIONS ---

    fun updateThemeMode(mode: Int) = viewModelScope.launch {
        dataStore.edit { it[THEME_MODE] = mode }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun updateNotifications(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    fun updateFallDetection(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[FALL_DETECTION] = enabled }
    }

    fun updateHighSensitivity(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[HIGH_SENSITIVITY] = enabled }
    }

    fun updateSendSos(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[SEND_SOS] = enabled }
    }

    fun updateEmergencyNumber(number: String) = viewModelScope.launch {
        dataStore.edit { it[EMERGENCY_NUMBER] = number }
    }

    fun updateAttachPictures(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[ATTACH_PICTURES] = enabled }
    }

    fun updateAttachAudio(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[ATTACH_AUDIO] = enabled }
    }

    fun addEmergencyContact(contactData: String) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val currentContacts = prefs[EMERGENCY_CONTACTS] ?: emptySet()
            prefs[EMERGENCY_CONTACTS] = currentContacts + contactData
        }
    }

    fun removeEmergencyContact(contactData: String) = viewModelScope.launch {
        dataStore.edit { prefs ->
            val currentContacts = prefs[EMERGENCY_CONTACTS] ?: emptySet()
            prefs[EMERGENCY_CONTACTS] = currentContacts - contactData
        }
    }

    // New Medical Info Update Function (Dynamic)
    fun updateMedicalField(key: Preferences.Key<String>, value: String) = viewModelScope.launch {
        dataStore.edit { it[key] = value }
    }

    fun updateShareDuringEmergency(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[SHARE_DURING_EMERGENCY] = enabled }
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
            _isUploading.value = true
            _errorMessage.value = null
            try {
                try {
                    storage.reference.child("avatars/${user.uid}.jpg").delete().await()
                } catch (e: Exception) {
                    // Ignore if file doesn't exist or was already deleted
                }

                // TODO: Clear local Room Database here

                dataStore.edit { it.clear() }

                user.delete().await()

                _isUploading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isUploading.value = false
                e.printStackTrace()
                _errorMessage.value = "Security Error: You must log out and log back in before deleting your account."
            }
        }
    }
}