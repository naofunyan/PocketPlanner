package com.example.pocketplanner.ui.chat.live

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000 // Gemini expects 16kHz
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> = flow {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        val buffer = ByteArray(2048) // Process in small chunks
        while (isRecording) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                // Yield copy of buffer to avoid mutation issues
                emit(buffer.copyOf(read))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
