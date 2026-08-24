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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    viewModel: SettingsViewModel = hiltViewModel() // Injected to read DataStore preferences
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(30) }

    // Safety flag to ensure we only trigger the SOS once
    var hasTriggered by remember { mutableStateOf(false) }

    // Collect DataStore settings silently in the background
    val contacts by viewModel.emergencyContacts.collectAsState()
    val isAudioEnabled by viewModel.attachAudioEnabled.collectAsState()
    val isPicturesEnabled by viewModel.attachPicturesEnabled.collectAsState()
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()

    // The Master Trigger Function
    val triggerSos = {
        if (!hasTriggered) {
            hasTriggered = true

            // 1. Fire the background manager
            val emergencyManager = EmergencyActionManager(context)
            emergencyManager.executeSosSequence(
                savedContacts = contacts,
                emergencyNumber = emergencyNumber,
                attachAudio = isAudioEnabled,
                attachPicture = isPicturesEnabled
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
            // Replaced the raw callback with our safe trigger
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
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Fall Detected!",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Notifying emergency contacts in...",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Giant Countdown Number
            Text(
                text = "$timeLeft",
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Swipe to Cancel Button
            SwipeToActionButton(
                text = "SWIPE TO CANCEL",
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onSwipeComplete = onCancel
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Swipe to SOS Button (Using an outline style so it looks distinct)
            SwipeToActionButton(
                text = "SWIPE FOR SOS",
                icon = Icons.Default.PhoneInTalk,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                isOutlined = true,
                onSwipeComplete = { triggerSos() } // Replaced raw callback
            )
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

        // Centered Text background
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Arrow hints to show it is a slider
        Icon(
            imageVector = Icons.Default.KeyboardDoubleArrowRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.3f),
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