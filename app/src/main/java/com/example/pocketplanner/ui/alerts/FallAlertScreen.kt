package com.example.pocketplanner.ui.alerts

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.ui.settings.SettingsViewModel
import com.example.pocketplanner.util.EmergencyActionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FallAlertScreen(
    onCancel: () -> Unit,
    onEmergencyTriggered: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(30) }

    // Safety flag to ensure we only trigger the SOS once
    var hasTriggered by remember { mutableStateOf(false) }

    // Collect standard emergency DataStore settings silently in the background
    val contacts by viewModel.emergencyContacts.collectAsState()
    val isAudioEnabled by viewModel.attachAudioEnabled.collectAsState()
    val isPicturesEnabled by viewModel.attachPicturesEnabled.collectAsState()
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()

    // Collect Medical DataStore settings
    val shareMedicalInfo by viewModel.shareDuringEmergencyEnabled.collectAsState()
    val name by viewModel.medicalName.collectAsState()
    val dob by viewModel.medicalDob.collectAsState()
    val weight by viewModel.medicalWeight.collectAsState()
    val height by viewModel.medicalHeight.collectAsState()
    val bloodType by viewModel.medicalBloodType.collectAsState()
    val allergies by viewModel.medicalAllergies.collectAsState()
    val conditions by viewModel.medicalConditions.collectAsState()
    val medications by viewModel.medicalMedications.collectAsState()
    val organDonor by viewModel.medicalOrganDonor.collectAsState()
    val address by viewModel.medicalAddress.collectAsState()
    val notes by viewModel.medicalNotes.collectAsState()

    // The Master Trigger Function
    val triggerSos = {
        if (!hasTriggered) {
            hasTriggered = true

            // Format a clean summary of the critical info
            val medicalSummary = buildString {
                if (bloodType.isNotBlank()) append("Blood: $bloodType | ")
                if (allergies.isNotBlank()) append("Allergies: $allergies | ")
                if (conditions.isNotBlank()) append("Conditions: $conditions | ")
                if (notes.isNotBlank()) append("Notes: $notes")
            }.trimEnd(' ', '|')

            // 1. Fire the background manager with the medical payload
            val emergencyManager = EmergencyActionManager(context)
            emergencyManager.executeSosSequence(
                savedContacts = contacts,
                emergencyNumber = emergencyNumber,
                attachAudio = isAudioEnabled,
                attachPicture = isPicturesEnabled,
                shareMedicalInfo = shareMedicalInfo,
                medicalSummary = medicalSummary
            )

            // 2. Notify the navigation graph to move to the success/confirmation screen
            onEmergencyTriggered()
        }
    }

    // Continuous vibration while the screen is active
    DisposableEffect(Unit) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }

        onDispose {
            vibrator.cancel()
        }
    }

    // Countdown Timer
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            triggerSos()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fall Detected!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Do you need help?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Notifying emergency contacts in...",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Giant Countdown Number with Circular Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = { timeLeft / 30f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    text = "$timeLeft",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Swipe to Cancel Button
            SwipeToActionButton(
                text = "SWIPE TO CANCEL",
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onSwipeComplete = onCancel
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Swipe to SOS Button (Using an outline style so it looks distinct)
            SwipeToActionButton(
                text = "SWIPE FOR SOS",
                icon = Icons.Default.PhoneInTalk,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                isOutlined = true,
                onSwipeComplete = { triggerSos() }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Medical Info Button
            var showMedicalDialog by remember { mutableStateOf(false) }
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f), // subtle background
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { showMedicalDialog = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "MEDICAL INFO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // Static Icon mimicking the draggable thumb
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onErrorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                }
            }

            if (showMedicalDialog) {
                AlertDialog(
                    onDismissRequest = { showMedicalDialog = false },
                    title = { Text("Medical Information", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            if (name.isNotBlank()) Text("Name: $name")
                            if (dob.isNotBlank()) Text("DOB: $dob")
                            if (weight.isNotBlank()) Text("Weight: $weight")
                            if (height.isNotBlank()) Text("Height: $height")
                            if (bloodType.isNotBlank()) Text("Blood Type: $bloodType")
                            if (allergies.isNotBlank()) Text("Allergies: $allergies")
                            if (conditions.isNotBlank()) Text("Conditions: $conditions")
                            if (medications.isNotBlank()) Text("Medications: $medications")
                            if (organDonor.isNotBlank()) Text("Organ Donor: $organDonor")
                            if (address.isNotBlank()) Text("Address: $address")
                            if (notes.isNotBlank()) Text("Notes: $notes")
                            
                            if (name.isBlank() && bloodType.isBlank() && allergies.isBlank() && conditions.isBlank() && 
                                medications.isBlank() && dob.isBlank() && weight.isBlank() && height.isBlank() && 
                                organDonor.isBlank() && address.isBlank() && notes.isBlank()) {
                                Text("No medical information provided.")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMedicalDialog = false }) {
                            Text("CLOSE")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Custom Compose UI element for a "Swipe to Action" slider button.
 */
@Composable
fun SwipeToActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    isOutlined: Boolean = false,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // State to track how far the thumb has been dragged
    val dragOffset = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    // Track the actual width of the container in pixels without BoxWithConstraints
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    var boxModifier = modifier
        .fillMaxWidth()
        .height(64.dp)
        .clip(CircleShape)
        .background(containerColor)
        .onSizeChanged { size ->
            containerWidthPx = size.width.toFloat() // Capture the width dynamically
        }

    if (isOutlined) {
        boxModifier = boxModifier.border(2.dp, contentColor, CircleShape)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbSizePx = with(density) { 64.dp.toPx() }
        // Ensure maxSwipePx never drops below 0 while the UI is rendering
        val maxSwipePx = (containerWidthPx - thumbSizePx).coerceAtLeast(0f)

        // Calculate alpha based on how far the thumb is dragged (fades out completely by halfway)
        val textAlpha = if (maxSwipePx > 0f) {
            val fadeEndPx = maxSwipePx * 0.5f
            (1f - (dragOffset.value / fadeEndPx)).coerceIn(0f, 1f)
        } else 1f

        // Centered Text background
        Text(
            text = text,
            color = contentColor.copy(alpha = textAlpha),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Arrow hints to show it is a slider
        Icon(
            imageVector = Icons.Default.KeyboardDoubleArrowRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = textAlpha * 0.3f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        )

        // The Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(64.dp)
                .padding(6.dp) // Inner padding
                .clip(CircleShape)
                .background(contentColor)
                .pointerInput(maxSwipePx) { // Re-initialize if the max width changes
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!isTriggered) {
                                coroutineScope.launch {
                                    // Spring back to start if not dragged far enough
                                    dragOffset.animateTo(0f)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()

                        // Only process drag if we know the container width and haven't triggered yet
                        if (!isTriggered && maxSwipePx > 0f) {
                            coroutineScope.launch {
                                val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxSwipePx)
                                dragOffset.snapTo(newOffset)

                                // Trigger if dragged past 85% of the bar
                                if (newOffset > maxSwipePx * 0.85f) {
                                    isTriggered = true
                                    dragOffset.animateTo(maxSwipePx)
                                    onSwipeComplete()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOutlined) MaterialTheme.colorScheme.errorContainer else containerColor
            )
        }
    }
}