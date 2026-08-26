package com.example.pocketplanner.ui.chat.interpreter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler

import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

@Composable
fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "vi" -> stringResource(R.string.interpreter_lang_vi)
        "en" -> stringResource(R.string.interpreter_lang_en)
        else -> stringResource(R.string.interpreter_lang_unknown)
    }
}

fun getLanguageSubtitle(code: String): String? {
    return when (code) {
        "vi" -> "Tiếng Việt"
        "en" -> "English"
        else -> null
    }
}

fun getPlaceholderText(code: String): String {
    return if (code == "vi") {
        "Chạm vào phím micro để bắt đầu hội thoại hoặc vào vùng văn bản để bắt đầu nhập văn bản cần dịch."
    } else {
        "Tap the mic button to start a voice conversation or the text area to start typing text to be translated."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpreterScreen(
    onNavigateBack: () -> Unit,
    viewModel: InterpreterViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val mode by viewModel.activeMode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val langA by viewModel.languageA.collectAsState()
    val langB by viewModel.languageB.collectAsState()

    // State to track if someone is actively typing
    var editingRole by remember { mutableStateOf<SpeakerRole?>(null) }

    // State for bottom sheet language picker
    var pickingLanguageFor by remember { mutableStateOf<SpeakerRole?>(null) }

    // State for screen rotation and history
    var isTopPanelFlipped by remember { mutableStateOf(true) }
    val topPanelRotation by animateFloatAsState(targetValue = if (isTopPanelFlipped) 180f else 0f)
    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // DYNAMIC BACKGROUND
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding() // Pushes the UI up when the keyboard opens!
    ) {
        if (editingRole != null) {
            // Intercept the Android hardware/gesture back button!
            BackHandler {
                editingRole = null // Dismiss text entry instead of leaving the screen
            }

            // --- FULL SCREEN TEXT ENTRY MODE ---
            TextEntryScreen(
                currentLanguage = if (editingRole == SpeakerRole.SPEAKER_A) langA else langB,
                onTranslate = { text ->
                    viewModel.sendText(text, editingRole!!)
                    editingRole = null // Close text entry and return to split screen
                },
                onCancel = { editingRole = null }
            )
        } else {
            // --- SPLIT SCREEN MODE ---
            val lastMessage = messages.lastOrNull()

            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // --- TOP HALF (Speaker A) ---
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .graphicsLayer(rotationZ = topPanelRotation), // ANIMATED FLIP
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface // DYNAMIC BACKGROUND
                ) {
                    SpeakerPanel(
                        role = SpeakerRole.SPEAKER_A,
                        languageName = getLanguageDisplayName(langA),
                        subLanguageName = getLanguageSubtitle(langA),
                        placeholderText = getPlaceholderText(langA),
                        lastMessage = lastMessage,
                        isMuted = isMuted,
                        onTextTap = { editingRole = SpeakerRole.SPEAKER_A },
                        onLanguagePress = { pickingLanguageFor = SpeakerRole.SPEAKER_A },
                        onMicPress = { viewModel.startVoiceRecording(SpeakerRole.SPEAKER_A) },
                        onMicRelease = { viewModel.stopVoiceRecording() },
                        onMuteToggle = { viewModel.toggleMute() },
                        onReplayPress = {
                            if (lastMessage != null) {
                                val text = if (lastMessage.speaker == SpeakerRole.SPEAKER_A) lastMessage.originalText else lastMessage.translatedText
                                viewModel.playTranslation(text, langA)
                            }
                        }
                    )
                }

                // --- MIDDLE CONTROLS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isTopPanelFlipped = !isTopPanelFlipped },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(48.dp) // DYNAMIC BACKGROUND
                    ) {
                        Icon(painter = painterResource(id = com.example.pocketplanner.R.drawable.flip), contentDescription = stringResource(R.string.interpreter_rotate_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                    }

                    IconButton(
                        onClick = { showHistory = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(48.dp) // DYNAMIC BACKGROUND
                    ) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.interpreter_history_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC ICON
                    }
                }

                // --- BOTTOM HALF (Speaker B) ---
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface // DYNAMIC BACKGROUND
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SpeakerPanel(
                            role = SpeakerRole.SPEAKER_B,
                            languageName = getLanguageDisplayName(langB),
                            subLanguageName = getLanguageSubtitle(langB),
                            placeholderText = getPlaceholderText(langB),
                            lastMessage = lastMessage,
                            isMuted = isMuted,
                            onTextTap = { editingRole = SpeakerRole.SPEAKER_B },
                            onLanguagePress = { pickingLanguageFor = SpeakerRole.SPEAKER_B },
                            onMicPress = { viewModel.startVoiceRecording(SpeakerRole.SPEAKER_B) },
                            onMicRelease = { viewModel.stopVoiceRecording() },
                            onMuteToggle = { viewModel.toggleMute() },
                            onReplayPress = {
                                if (lastMessage != null) {
                                    val text = if (lastMessage.speaker == SpeakerRole.SPEAKER_B) lastMessage.originalText else lastMessage.translatedText
                                    viewModel.playTranslation(text, langB)
                                }
                            }
                        )

                        // Tiny offline indicator if needed
                        if (mode == ConnectionMode.OFFLINE_MLKIT) {
                            Text(
                                text = stringResource(R.string.interpreter_offline_mode),
                                color = MaterialTheme.colorScheme.error, // DYNAMIC TEXT
                                fontSize = 10.sp,
                                modifier = Modifier.padding(16.dp).align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- HISTORY BOTTOM SHEET ---
    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = MaterialTheme.colorScheme.surface // DYNAMIC BACKGROUND
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.interpreter_history_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
                if (messages.isEmpty()) {
                    Text(stringResource(R.string.interpreter_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                } else {
                    messages.forEach { msg ->
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(msg.originalText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) // DYNAMIC TEXT
                            Text(msg.translatedText, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    // --- LANGUAGE PICKER BOTTOM SHEET ---
    if (pickingLanguageFor != null) {
        val isForMe = pickingLanguageFor == SpeakerRole.SPEAKER_B
        val currentLang = if (isForMe) langB else langA

        LanguagePickerBottomSheet(
            isForMe = isForMe,
            currentLanguageCode = currentLang,
            onLanguageSelected = { newCode ->
                if (isForMe) {
                    viewModel.setLanguageB(newCode)
                } else {
                    viewModel.setLanguageA(newCode)
                }
                pickingLanguageFor = null
            },
            onDismiss = { pickingLanguageFor = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerBottomSheet(
    isForMe: Boolean,
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (isForMe) stringResource(R.string.interpreter_my_language) else stringResource(R.string.interpreter_other_language)
    val languages = listOf(
        "vi" to stringResource(R.string.interpreter_lang_vi),
        "en" to stringResource(R.string.interpreter_lang_en)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface // DYNAMIC BACKGROUND
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 16.dp)
            )
            languages.forEach { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelected(code) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentLanguageCode == code,
                        onClick = { onLanguageSelected(code) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) // DYNAMIC TEXT
                }
            }
        }
    }
}

@Composable
fun SpeakerPanel(
    role: SpeakerRole,
    languageName: String,
    subLanguageName: String? = null,
    placeholderText: String,
    lastMessage: InterpreterMessage?,
    isMuted: Boolean,
    onTextTap: () -> Unit,
    onLanguagePress: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onReplayPress: () -> Unit,
    onMuteToggle: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Middle Area (Text / Conversation)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTextTap
                ),
            contentAlignment = Alignment.TopStart
        ) {
            if (lastMessage == null) {
                Text(
                    text = placeholderText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), // DYNAMIC TEXT
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (lastMessage.speaker == role) {
                    Text(
                        text = lastMessage.originalText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                        fontSize = 24.sp,
                        lineHeight = 32.sp
                    )
                } else {
                    Text(
                        text = lastMessage.translatedText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 40.sp
                    )
                }
            }
        }

        // Bottom Controls (Speaker, Language, Mic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replay and Mute Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onReplayPress,
                    modifier = Modifier.size(48.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) // DYNAMIC BORDER
                ) {
                    Icon(Icons.Default.Replay, contentDescription = stringResource(R.string.interpreter_replay_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier.size(48.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) // DYNAMIC BORDER
                ) {
                    val icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp
                    val tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary // DYNAMIC ICON
                    Icon(icon, contentDescription = stringResource(R.string.interpreter_mute_cd), tint = tint, modifier = Modifier.size(24.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLanguagePress() }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = languageName, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (subLanguageName != null) {
                        Text(text = subLanguageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) // DYNAMIC TEXT
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.interpreter_select_lang_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)) // DYNAMIC ICON
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) // DYNAMIC BORDER
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onMicPress()
                                tryAwaitRelease()
                                onMicRelease()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.interpreter_speak_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun TextEntryScreen(
    currentLanguage: String,
    onTranslate: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val isVietnamese = currentLanguage == "vi"
    val placeholderText = if (isVietnamese) "Nhập văn bản" else "Enter text"
    val clearText = if (isVietnamese) "Xóa" else "Clear"
    val translateText = if (isVietnamese) "Dịch" else "Translate"

    Surface(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface // DYNAMIC BACKGROUND
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                    textStyle = TextStyle(fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface), // DYNAMIC TEXT
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
                if (text.isEmpty()) {
                    Text(placeholderText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp) // DYNAMIC TEXT
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { if (text.isEmpty()) onCancel() else text = "" }) {
                    Text(clearText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // DYNAMIC TEXT
                }
                Button(
                    onClick = { onTranslate(text) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = text.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(translateText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Auto-focus the keyboard when this screen appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}