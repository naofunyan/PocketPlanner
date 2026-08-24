package com.example.pocketplanner.ui.chat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.pocketplanner.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    // State for Image Picker
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            // Convert URI to Bitmap
            selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedImageUri = null // Clear URI since it came directly from camera
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                inputText = if (inputText.isEmpty()) spokenText else "$inputText $spokenText"
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant", color = MaterialTheme.colorScheme.onBackground) }, // DYNAMIC TEXT
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) // DYNAMIC ICON
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background) // DYNAMIC BACKGROUND
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Chat History
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(message)
                }
            }

            // Input Bar
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) { // DYNAMIC BACKGROUND
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    // Image Preview Area
                    if (selectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, bottom = 12.dp)
                                .size(80.dp)
                        ) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Attached Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // Clean small 'X' button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(22.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape) // DYNAMIC COLOR
                                    .clickable {
                                        selectedImageUri = null
                                        selectedBitmap = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove Image", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp)) // DYNAMIC ICON
                            }
                        }
                    }

                    // Text Input & Buttons (Modern Pill Design)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)) // DYNAMIC BACKGROUND
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.addphoto),
                            contentDescription = "Gallery",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC ICON
                            modifier = Modifier
                                .padding(start = 12.dp, bottom = 12.dp, end = 4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { galleryLauncher.launch("image/*") }
                                .padding(2.dp)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.camera),
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC ICON
                            modifier = Modifier
                                .padding(start = 4.dp, bottom = 12.dp, end = 4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { cameraLauncher.launch(null) }
                                .padding(2.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 14.dp, bottom = 14.dp, start = 8.dp, end = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text("Ask anything...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 16.sp) // DYNAMIC TEXT
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface), // DYNAMIC TEXT
                                maxLines = 4,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Crossfade(
                            targetState = inputText.isNotBlank() || selectedBitmap != null,
                            label = "SendOrMic",
                            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
                        ) { isReadyToSend ->
                            if (isReadyToSend) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape) // DYNAMIC BUTTON
                                        .clickable {
                                            viewModel.sendMessage(inputText, selectedBitmap)
                                            inputText = ""
                                            selectedImageUri = null
                                            selectedBitmap = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) // DYNAMIC ICON
                                }
                            } else {
                                IconButton(onClick = {
                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                    }
                                    try {
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Speech recognition not available", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC ICON
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    // User gets primary color, AI gets surfaceVariant color
    val backgroundColor = if (message.isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isFromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.text))
                                android.widget.Toast.makeText(context, "Message copied", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .padding(16.dp)
            ) {
                // If there's an image, render it above the text
                if (message.imageBitmap != null) {
                    Image(
                        bitmap = message.imageBitmap.asImageBitmap(),
                        contentDescription = "Message Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = if (message.text.isNotBlank()) 8.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = parseMarkdown(message.text),
                        color = textColor
                    )
                }
            }
        }
    }
}

fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentIndex = 0
        // Regex to catch **bold** and *italic*
        val regex = Regex("\\*\\*(.*?)\\*\\*|\\*(.*?)\\*")
        val matches = regex.findAll(text)

        for (match in matches) {
            append(text.substring(currentIndex, match.range.first))
            if (match.groupValues[1].isNotEmpty()) {
                // Bold
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                append(match.groupValues[1])
                pop()
            } else if (match.groupValues[2].isNotEmpty()) {
                // Italic
                pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                append(match.groupValues[2])
                pop()
            }
            currentIndex = match.range.last + 1
        }
        append(text.substring(currentIndex))
    }
}