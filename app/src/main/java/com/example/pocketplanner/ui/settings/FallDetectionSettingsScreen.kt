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
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

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
                    Text(stringResource(id = R.string.fall_detection_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.settings_cd_back))
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
                        text = if (isEnabled) stringResource(id = R.string.fall_detection_on) else stringResource(id = R.string.fall_detection_off),
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
                        text = stringResource(id = R.string.fall_detection_info_1),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.fall_detection_info_2),
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
                            text = stringResource(id = R.string.fall_detection_high_sensitivity),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.fall_detection_high_sensitivity_desc),
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
                        text = stringResource(id = R.string.fall_detection_emergency_number),
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
                                text = stringResource(id = R.string.fall_detection_send_sos),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.fall_detection_send_sos_desc),
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
                        text = stringResource(id = R.string.fall_detection_emergency_sharing_settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, // Blue interactive text
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToEmergencySharing() }
                    )
                }
            }

            // 6. Test Fall Alert Button
            Button(
                onClick = {
                    val intent = android.content.Intent(context, com.example.pocketplanner.ui.alerts.EmergencyAlertActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.WarningAmber, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.fall_detection_test_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

        }

        // --- DIALOG FOR EDITING NUMBER ---
        if (showNumberDialog) {
            AlertDialog(
                onDismissRequest = { showNumberDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = stringResource(id = R.string.fall_detection_dialog_title),
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
                            label = { Text(stringResource(id = R.string.fall_detection_dialog_label)) },
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
                            Text(stringResource(id = R.string.fall_detection_dialog_revert), color = MaterialTheme.colorScheme.primary)
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
                        Text(stringResource(id = R.string.fall_detection_dialog_save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNumberDialog = false }) {
                        Text(stringResource(id = R.string.fall_detection_dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}