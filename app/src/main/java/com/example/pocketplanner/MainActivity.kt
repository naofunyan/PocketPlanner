package com.example.pocketplanner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.pocketplanner.ui.ml.FallDetectionService
import com.example.pocketplanner.ui.alerts.FallAlertScreen
import com.example.pocketplanner.ui.navigation.MainScreen
import com.example.pocketplanner.ui.settings.SettingsViewModel
import com.example.pocketplanner.ui.theme.PocketPlannerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.view.WindowManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // 1. Inject the DataStore setup from your AppModule
    @Inject
    lateinit var dataStore: DataStore<Preferences>

    // State to control when the giant red alert screen appears
    private var showFallAlertState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NEW: Tell Android to wake the screen and show over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // Keep the screen on while the countdown is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Check if the app was launched by the FallDetectionService
        if (intent?.getBooleanExtra("TRIGGER_FALL_ALERT", false) == true) {
            showFallAlertState.value = true
            intent?.removeExtra("TRIGGER_FALL_ALERT")
        }

        // 2. Read the saved theme and apply it immediately on launch
        lifecycleScope.launch {
            dataStore.data.collect { prefs ->
                val savedTheme = prefs[intPreferencesKey("theme_mode")] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(savedTheme)
            }
        }

        setContent {
            val context = LocalContext.current

            // Grab the SettingsViewModel to observe the fall detection toggle
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isFallDetectionEnabled by settingsViewModel.fallDetectionEnabled.collectAsState()

            // Start or Stop the service whenever the toggle changes!
            LaunchedEffect(isFallDetectionEnabled) {
                val serviceIntent = Intent(context, FallDetectionService::class.java)
                if (isFallDetectionEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    context.stopService(serviceIntent)
                }
            }

            PocketPlannerTheme {
                if (showFallAlertState.value) {
                    // Show the giant red countdown screen over everything
                    FallAlertScreen(
                        onCancel = { showFallAlertState.value = false },
                        onEmergencyTriggered = {
                            showFallAlertState.value = false
                            // TODO: Trigger actual SMS/Call logic here
                        }
                    )
                } else {
                    // Show your normal MainScreen()
                    MainScreen()
                }
            }
        }
    }

    // Catch the intent if the app is already open in the background/foreground
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("TRIGGER_FALL_ALERT", false)) {
            showFallAlertState.value = true
            intent.removeExtra("TRIGGER_FALL_ALERT")
        }
    }
}