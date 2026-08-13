package com.example.pocketplanner.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()
    val formatter = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Alerts", fontWeight = FontWeight.Bold, color = Color(0xFF005b9f)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFFAFAFA))
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No proximity alerts yet. Go explore!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts) { alert ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (alert.isRead) Color.White else Color(0xFFE3F2FD),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFF005b9f).copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF005b9f), modifier = Modifier.padding(12.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(alert.placeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(alert.message, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(formatter.format(Date(alert.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}