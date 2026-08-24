package com.example.pocketplanner.ui.alerts

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pocketplanner.ui.theme.PocketPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force the screen to turn on and bypass the lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            PocketPlannerTheme {
                FallAlertScreen(
                    onCancel = { finish() }, // Close activity if canceled
                    onEmergencyTriggered = { finish() } // Close activity after SOS fires
                )
            }
        }
    }
}