package com.example.pocketplanner.ui.chat

import java.util.UUID
import android.graphics.Bitmap

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val imageBitmap: Bitmap? = null
)