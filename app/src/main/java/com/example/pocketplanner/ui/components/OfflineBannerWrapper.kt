package com.example.pocketplanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketplanner.core.offline.NetworkMonitor

@Composable
fun OfflineBannerWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }

    // Default to true (online) so we don't flash the banner on cold start
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error) // Red offline color
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.WifiOff, contentDescription = "Offline", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("You're offline. Changes will sync later.", color = MaterialTheme.colorScheme.onError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Render the rest of the app below the banner
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}