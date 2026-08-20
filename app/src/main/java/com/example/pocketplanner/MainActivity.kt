package com.example.pocketplanner
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pocketplanner.ui.navigation.MainScreen
import com.example.pocketplanner.ui.theme.PocketPlannerTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketPlannerTheme {
                MainScreen()
            }
        }
    }
}