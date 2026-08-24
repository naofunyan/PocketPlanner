package com.example.pocketplanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallDetectionSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmergencySharing: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isEnabled by viewModel.fallDetectionEnabled.collectAsState()
    val isHighSensitivity by viewModel.highSensitivityEnabled.collectAsState()
    val isSendSosEnabled by viewModel.sendSosEnabled.collectAsState()
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()

    val context = LocalContext.current

    // State for the edit dialog
    var showNumberDialog by remember { mutableStateOf(false) }
    var tempNumber by remember { mutableStateOf(emergencyNumber) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Hard fall detection", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                // INCREASED BOTTOM PADDING TO CLEAR NAVIGATION BAR
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Master Toggle Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnabled) "On" else "Off",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isChecked ->
                            // 1. Save the new state to DataStore
                            viewModel.updateFallDetection(isChecked)

                            // 2. Prepare the Intent for the background service
                            val serviceIntent = android.content.Intent(
                                context,
                                com.example.pocketplanner.ui.ml.FallDetectionService::class.java
                            ).apply {
                                putExtra("HIGH_SENSITIVITY", isHighSensitivity)
                            }

                            // 3. Start or Stop the service based on the toggle
                            if (isChecked) {
                                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                            } else {
                                context.stopService(serviceIntent)
                            }
                        }
                    )
                }
            }

            // 2. Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(60.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "After a fall, your phone will alert you for 30 seconds with a full-screen popup, sound, and heavy vibration. You can cancel the alert or swipe to send SOS messages right away.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "If you don't respond for 30 seconds, SOS messages will be sent to the emergency contacts you set. The message will include your current location.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp
                    )
                }
            }

            // 3. High Sensitivity Toggle Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High sensitivity",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "More falls will be detected, but sudden movements or dropping your phone may also trigger false alarms. Best for people who have a high risk of falling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isHighSensitivity,
                        onCheckedChange = { viewModel.updateHighSensitivity(it) },
                        enabled = isEnabled
                    )
                }
            }

            // 4. Emergency Number to Call
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) {
                        tempNumber = emergencyNumber // Reset temp state to current saved number
                        showNumberDialog = true
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Emergency number to call",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emergencyNumber, // Now dynamically shows the saved number!
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 5. Send SOS to Emergency Contacts
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Send SOS to emergency contacts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Text your location and an SOS message to your emergency contacts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isSendSosEnabled,
                            onCheckedChange = { viewModel.updateSendSos(it) },
                            enabled = isEnabled
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Emergency sharing settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, // Blue interactive text
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToEmergencySharing() }
                    )
                }
            }

        }

        // --- DIALOG FOR EDITING NUMBER ---
        if (showNumberDialog) {
            AlertDialog(
                onDismissRequest = { showNumberDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Emergency number",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tempNumber,
                            onValueChange = { newValue ->
                                // Only allow digits and the + symbol
                                if (newValue.all { it.isDigit() || it == '+' }) {
                                    tempNumber = newValue
                                }
                            },
                            label = { Text("Number to call") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // The "Reset to Default" button tucked neatly under the text field
                        TextButton(
                            onClick = { tempNumber = "113" },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Revert to default (113)", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // If they leave it completely blank, automatically save it as 113 for safety
                            val finalNumber = if (tempNumber.isNotBlank()) tempNumber else "113"
                            viewModel.updateEmergencyNumber(finalNumber)
                            showNumberDialog = false
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNumberDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}