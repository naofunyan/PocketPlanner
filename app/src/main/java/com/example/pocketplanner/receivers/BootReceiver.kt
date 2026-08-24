package com.example.pocketplanner.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.pocketplanner.ui.ml.FallDetectionService
import com.example.pocketplanner.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Safe top-level initialization if you aren't using Hilt for the receiver
val Context.dataStore by preferencesDataStore(name = "settings")

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Launch a quick coroutine to check DataStore safely
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = context.dataStore.data.first()
                val isEnabled = prefs[booleanPreferencesKey("fall_detection")] ?: false
                val isHighSensitivity = prefs[booleanPreferencesKey("high_sensitivity")] ?: false

                if (isEnabled) {
                    val serviceIntent = Intent(context, FallDetectionService::class.java).apply {
                        putExtra("HIGH_SENSITIVITY", isHighSensitivity)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }
}