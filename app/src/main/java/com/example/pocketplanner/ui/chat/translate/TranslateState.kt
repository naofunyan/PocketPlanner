package com.example.pocketplanner.ui.chat.translate

import android.graphics.Bitmap
import android.graphics.Rect

enum class TranslateMode {
    LIVE,      // Real-time camera preview
    CAPTURED,  // Frozen camera frame
    GALLERY    // Image picked from device
}

data class DetectedTextBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect?, // Keep for backwards compatibility
    val lineCount: Int = 1,
    val cornerX: Float = 0f,
    val cornerY: Float = 0f,
    val textWidth: Float = 0f,
    val textHeight: Float = 0f,
    val rotationAngle: Float = 0f
)

data class TranslateUiState(
    val mode: TranslateMode = TranslateMode.LIVE,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en",
    val blocks: List<DetectedTextBlock> = emptyList(),
    val capturedImage: Bitmap? = null,
    val isProcessing: Boolean = false,
    val sourceImageWidth: Int = 1,
    val sourceImageHeight: Int = 1,
    val spokenBlockIndex: Int? = null,
    val spokenLocalRange: IntRange? = null
)
