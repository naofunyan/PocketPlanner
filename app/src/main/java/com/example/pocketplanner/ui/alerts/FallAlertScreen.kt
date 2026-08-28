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
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R
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
                if (bloodType.isNotBlank()) append("${context.getString(R.string.fall_alert_summary_blood)}: $bloodType | ")
                if (allergies.isNotBlank()) append("${context.getString(R.string.fall_alert_summary_allergies)}: $allergies | ")
                if (conditions.isNotBlank()) append("${context.getString(R.string.fall_alert_summary_conditions)}: $conditions | ")
                if (notes.isNotBlank()) append("${context.getString(R.string.fall_alert_summary_notes)}: $notes")
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

    // Flashlight & Sound State
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isSoundPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember { 
        try { 
            cameraManager?.cameraIdList?.firstOrNull() 
        } catch (e: Exception) { 
            null 
        } 
    }

    fun toggleFlashlight() {
        try {
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager?.setTorchMode(cameraId, isFlashlightOn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleSound() {
        if (isSoundPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isSoundPlaying = false
        } else {
            try {
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer.create(context, alarmUri).apply {
                    isLooping = true
                    start()
                }
                isSoundPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Cleanup resources
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            try {
                if (cameraId != null) {
                    cameraManager?.setTorchMode(cameraId, false)
                }
            } catch (e: Exception) {}
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
                contentDescription = stringResource(R.string.fall_alert_warning_icon_desc),
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.fall_alert_detected_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.fall_alert_need_help),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.fall_alert_notifying),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Flashlight and Alarm Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { toggleFlashlight() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFlashlightOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                        contentColor = if (isFlashlightOn) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFlashlightOn) stringResource(R.string.fall_alert_flash_off) else stringResource(R.string.fall_alert_flash_on), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { toggleSound() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSoundPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSoundPlaying) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSoundPlaying) stringResource(R.string.fall_alert_sound_stop) else stringResource(R.string.fall_alert_sound_play), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Swipe to Cancel Button
            SwipeToActionButton(
                text = stringResource(R.string.fall_alert_swipe_cancel),
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onSwipeComplete = onCancel
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Swipe to SOS Button (Using an outline style so it looks distinct)
            SwipeToActionButton(
                text = stringResource(R.string.fall_alert_swipe_sos),
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
                        text = stringResource(R.string.fall_alert_medical_info_button),
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
                    title = { Text(stringResource(R.string.fall_alert_medical_info_title), fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            if (name.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_name_prefix, name))
                            if (dob.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_dob_prefix, dob))
                            if (weight.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_weight_prefix, weight))
                            if (height.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_height_prefix, height))
                            if (bloodType.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_blood_type_prefix, bloodType))
                            if (allergies.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_allergies_prefix, allergies))
                            if (conditions.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_conditions_prefix, conditions))
                            if (medications.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_medications_prefix, medications))
                            if (organDonor.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_organ_donor_prefix, organDonor))
                            if (address.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_address_prefix, address))
                            if (notes.isNotBlank()) Text(stringResource(R.string.fall_alert_medical_notes_prefix, notes))
                            
                            if (name.isBlank() && bloodType.isBlank() && allergies.isBlank() && conditions.isBlank() && 
                                medications.isBlank() && dob.isBlank() && weight.isBlank() && height.isBlank() && 
                                organDonor.isBlank() && address.isBlank() && notes.isBlank()) {
                                Text(stringResource(R.string.fall_alert_no_medical_info))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMedicalDialog = false }) {
                            Text(stringResource(R.string.fall_alert_close))
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