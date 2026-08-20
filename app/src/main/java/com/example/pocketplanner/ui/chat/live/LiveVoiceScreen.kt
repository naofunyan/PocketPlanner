package com.example.pocketplanner.ui.chat.live

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam

import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideocamOff

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveVoiceScreen(
    onNavigateBack: () -> Unit,
    isActive: Boolean = true,
    viewModel: LiveVoiceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isCameraOff by viewModel.isCameraOff.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    )

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    // Pass context to ViewModel for service account auth
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    // Auto-start when permissions are granted and the tab is active
    LaunchedEffect(permissionsState.allPermissionsGranted, isActive) {
        if (!permissionsState.allPermissionsGranted) {
            if (isActive) permissionsState.launchMultiplePermissionRequest()
        } else {
            if (isActive && state == LiveVoiceState.IDLE) {
                viewModel.startCall()
            } else if (!isActive && state != LiveVoiceState.IDLE) {
                viewModel.endCall()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // Camera Background
        if (permissionsState.allPermissionsGranted) {
            val previewView = remember { PreviewView(context) }

            LaunchedEffect(lensFacing, isCameraOff, lifecycleOwner, isActive) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    if (isActive && !isCameraOff) {
                        // Only unbind all if WE are the active tab taking control!
                        cameraProvider.unbindAll()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    try {
                                        val bitmap = imageProxy.toBitmap()
                                        val stream = ByteArrayOutputStream()
                                        val scaled = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                                        scaled.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                                        viewModel.sendVideoFrame(stream.toByteArray())
                                    } catch (e: Exception) {
                                        Log.e("LiveVoice", "Frame error", e)
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                        try {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageAnalyzer
                            )
                        } catch (e: Exception) {
                            Log.e("LiveVoice", "Camera bind error", e)
                        }
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            if (!isCameraOff) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Error message if any
        val errorMessage by viewModel.errorMessage.collectAsState()
        if (state == LiveVoiceState.ERROR && errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .navigationBarsPadding()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.8f), CircleShape)
                    .clickable { 
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Flip Camera", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isCameraOff) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E).copy(alpha = 0.8f), CircleShape)
                    .clickable { viewModel.toggleCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam, 
                    contentDescription = if (isCameraOff) "Turn Camera On" else "Turn Camera Off", 
                    tint = Color.White
                )
            }
            
            VoiceVisualizerPill(
                state = state,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isMuted) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E).copy(alpha = 0.8f), CircleShape)
                    .clickable { viewModel.toggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, 
                    contentDescription = if (isMuted) "Unmute" else "Mute", 
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VoiceVisualizerPill(state: LiveVoiceState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    // Drifting X positions for 3 overlapping blobs to create an "aurora" effect
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "x1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "x2"
    )
    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "x3"
    )

    // Base scale based on voice state instead of fixed DP
    val targetBaseScale = when (state) {
        LiveVoiceState.CONNECTING -> 0.6f
        LiveVoiceState.LISTENING -> 1.0f
        LiveVoiceState.SPEAKING -> 1.4f // Keeps the radius contained
        else -> 0f
    }
    
    // Smooth transition between states
    val baseScale by animateFloatAsState(
        targetValue = targetBaseScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 50f),
        label = "baseScale"
    )

    // Fast pulsing multiplier when speaking, slow breathing when listening
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == LiveVoiceState.SPEAKING) 300 else 1000, 
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val c1 = if (state == LiveVoiceState.ERROR) errorColor else primaryColor
    val c2 = if (state == LiveVoiceState.ERROR) errorColor else tertiaryColor
    val c3 = if (state == LiveVoiceState.ERROR) errorColor else secondaryColor

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF151515).copy(alpha = 0.9f))
            .drawBehind {
                if (baseScale > 0f) {
                    // Radius relative to height ensures it never completely covers the pill
                    val finalRadius = size.height * baseScale * pulse
                    
                    // Blob 1 (Primary)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c1.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(size.width * (0.2f + 0.6f * offset1), size.height * 0.7f),
                            radius = finalRadius
                        ),
                        center = Offset(size.width * (0.2f + 0.6f * offset1), size.height * 0.7f),
                        radius = finalRadius
                    )
                    
                    // Blob 2 (Tertiary)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c2.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(size.width * (0.1f + 0.8f * offset2), size.height),
                            radius = finalRadius * 0.9f
                        ),
                        center = Offset(size.width * (0.1f + 0.8f * offset2), size.height),
                        radius = finalRadius * 0.9f
                    )

                    // Blob 3 (Secondary)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c3.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * (0.3f + 0.4f * offset3), size.height * 1.1f),
                            radius = finalRadius * 1.1f
                        ),
                        center = Offset(size.width * (0.3f + 0.4f * offset3), size.height * 1.1f),
                        radius = finalRadius * 1.1f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (state == LiveVoiceState.CONNECTING) {
            Text(
                text = "Connecting...", 
                color = Color.White.copy(alpha = 0.9f), 
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
