package com.example.pocketplanner.ui.chat.interpreter

enum class SpeakerRole { SPEAKER_A, SPEAKER_B }

enum class ConnectionMode { ONLINE_GEMINI, OFFLINE_MLKIT }

data class InterpreterMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: SpeakerRole,
    val originalText: String,
    val translatedText: String,
    val isFinal: Boolean = true // False while Gemini is actively streaming the translation
)