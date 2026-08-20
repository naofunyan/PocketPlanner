package com.example.pocketplanner.ui.chat.interpreter

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.core.utils.NetworkConnectivityObserver
import com.example.pocketplanner.ui.chat.TranslationManager
import com.example.pocketplanner.ui.chat.live.AudioPlayer
import com.example.pocketplanner.ui.chat.live.AudioRecorder
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class InterpreterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationManager: TranslationManager,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel(), TextToSpeech.OnInitListener {

    private val _messages = MutableStateFlow<List<InterpreterMessage>>(emptyList())
    val messages: StateFlow<List<InterpreterMessage>> = _messages

    private val _languageA = MutableStateFlow("en")
    val languageA: StateFlow<String> = _languageA

    private val _languageB = MutableStateFlow("vi")
    val languageB: StateFlow<String> = _languageB

    fun setLanguageA(lang: String) {
        _languageA.value = lang
        // Automatically swap the other language if there is a collision
        if (_languageB.value == lang) {
            _languageB.value = if (lang == "en") "vi" else "en"
        }
    }

    fun setLanguageB(lang: String) {
        _languageB.value = lang
        // Automatically swap the other language if there is a collision
        if (_languageA.value == lang) {
            _languageA.value = if (lang == "en") "vi" else "en"
        }
    }

    private val _activeMode = MutableStateFlow(ConnectionMode.OFFLINE_MLKIT)
    val activeMode: StateFlow<ConnectionMode> = _activeMode

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private var isCurrentlyOnline = false
    private var tts: TextToSpeech? = null

    // Gemini Live API Dependencies
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var webSocket: WebSocket? = null
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer()
    private var isGeminiSetupComplete = false

    // Buffers to hold Gemini's streaming response before pushing to the UI
    private var currentGeminiText = ""
    private var currentSpeaker: SpeakerRole? = null
    private var currentOriginalText: String = ""

    init {
        tts = TextToSpeech(context, this)

        viewModelScope.launch(Dispatchers.Main) {
            audioPlayer.init()
        }

        // Dynamically listen to network changes
        viewModelScope.launch {
            networkObserver.isOnline.collectLatest { online ->
                isCurrentlyOnline = online
                _activeMode.value = if (online) ConnectionMode.ONLINE_GEMINI else ConnectionMode.OFFLINE_MLKIT

                // Automatically connect to Gemini if we are online and not connected
                if (online && webSocket == null) {
                    connectToGeminiLive()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("vi")
        }
    }

    private fun connectToGeminiLive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get the secure OAuth token from Firebase Functions
                val result = Firebase.functions.getHttpsCallable("getVertexAccessToken").call().await()
                val accessToken = (result.data as? Map<String, Any>)?.get("token") as? String ?: return@launch

                val request = Request.Builder()
                    .url("wss://us-central1-aiplatform.googleapis.com/ws/google.cloud.aiplatform.v1.LlmBidiService/BidiGenerateContent")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val setupMsg = JSONObject().apply {
                            put("setup", JSONObject().apply {
                                put("model", "projects/pocketplanner-b9422/locations/us-central1/publishers/google/models/gemini-live-2.5-flash-native-audio")

                                // CRITICAL: Request BOTH AUDIO and TEXT so we get the voice AND the transcription
                                put("generationConfig", JSONObject().apply {
                                    put("responseModalities", JSONArray().apply {
                                        put("AUDIO")
                                        put("TEXT")
                                    })
                                })

                                put("systemInstruction", JSONObject().apply {
                                    put("parts", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("text", "You are an interpreter. Translate English to Vietnamese, and Vietnamese to English. Output ONLY the translation. Speak the translation out loud naturally.")
                                        })
                                    })
                                })
                            })
                        }
                        // IMPORTANT: Unescape forward slashes for Vertex AI!
                        webSocket.send(setupMsg.toString().replace("\\/", "/"))
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                        onMessage(webSocket, bytes.utf8())
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            if (json.has("setupComplete")) {
                                Log.d("Interpreter", "Gemini WebSocket Connected!")
                                isGeminiSetupComplete = true
                            }

                            if (json.has("serverContent")) {
                                val serverContent = json.getJSONObject("serverContent")
                                if (serverContent.has("modelTurn")) {
                                    val parts = serverContent.getJSONObject("modelTurn").getJSONArray("parts")
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)

                                        // A. Play the streamed audio translation
                                        if (part.has("inlineData") && !_isMuted.value) {
                                            val pcmBytes = Base64.decode(part.getJSONObject("inlineData").getString("data"), Base64.DEFAULT)
                                            audioPlayer.play(pcmBytes)
                                        }

                                        // B. Collect the streamed text translation
                                        if (part.has("text")) {
                                            currentGeminiText += part.getString("text")

                                            // LIVE STREAMING TEXT TO UI!
                                            if (currentSpeaker != null) {
                                                val updatedList = _messages.value.toMutableList()
                                                val lastIndex = updatedList.indexOfLast { it.speaker == currentSpeaker && !it.isFinal }
                                                if (lastIndex != -1) {
                                                    updatedList[lastIndex] = updatedList[lastIndex].copy(
                                                        translatedText = currentGeminiText
                                                    )
                                                    _messages.value = updatedList
                                                }
                                            }
                                        }
                                    }
                                }

                                // C. When Gemini finishes its turn, mark as final
                                if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                                    if (currentSpeaker != null) {
                                        val updatedList = _messages.value.toMutableList()
                                        val lastIndex = updatedList.indexOfLast { it.speaker == currentSpeaker && !it.isFinal }
                                        if (lastIndex != -1) {
                                            updatedList[lastIndex] = updatedList[lastIndex].copy(
                                                translatedText = currentGeminiText.trim(),
                                                isFinal = true
                                            )
                                            _messages.value = updatedList
                                        }

                                        // Reset buffer for the next translation
                                        currentGeminiText = ""
                                        currentSpeaker = null
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Interpreter", "Gemini Parse Error", e)
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d("Interpreter", "WebSocket closed: $reason")
                        isGeminiSetupComplete = false
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e("Interpreter", "WebSocket Failure", t)
                        isGeminiSetupComplete = false
                    }
                })
            } catch (e: Exception) {
                Log.e("Interpreter", "Failed to connect to Gemini", e)
                isGeminiSetupComplete = false
            }
        }
    }

    /**
     * Called when the user types text and hits "Send"
     */
    fun sendText(text: String, speaker: SpeakerRole) {
        val sourceLang = if (speaker == SpeakerRole.SPEAKER_A) _languageA.value else _languageB.value
        val targetLang = if (speaker == SpeakerRole.SPEAKER_A) _languageB.value else _languageA.value

        // INSTANT UI FEEDBACK: Show the user's text immediately with a "..." placeholder for translation
        val initialMsg = InterpreterMessage(
            speaker = speaker,
            originalText = text,
            translatedText = "...",
            isFinal = false // False means it is actively translating
        )
        _messages.value = _messages.value + initialMsg

        // If online AND the socket successfully connected, use Gemini
        if (isCurrentlyOnline && isGeminiSetupComplete) {
            // ONLINE GEMINI

            // Set up our state so when Gemini streams the response back, we know who it belongs to
            currentSpeaker = speaker
            currentOriginalText = text
            currentGeminiText = ""

            val msg = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", "Translate this: $text") })
                            })
                        })
                    })
                    put("turnComplete", true) // Tell Gemini to start translating immediately
                })
            }
            webSocket?.send(msg.toString())
        } else {
            // OFFLINE ML KIT FALLBACK (Also gracefully falls back here if the WebSocket failed to connect!)
            Log.d("Interpreter", "Falling back to local ML Kit translation")
            viewModelScope.launch {
                val translated = translationManager.translate(text, sourceLang.split("-")[0], targetLang.split("-")[0])

                // Find our placeholder message and update it
                val updatedList = _messages.value.toMutableList()
                val lastIndex = updatedList.indexOfLast { it.speaker == speaker && !it.isFinal }
                if (lastIndex != -1) {
                    updatedList[lastIndex] = updatedList[lastIndex].copy(
                        translatedText = translated,
                        isFinal = true
                    )
                    _messages.value = updatedList
                }

                if (!_isMuted.value) {
                    tts?.language = Locale.forLanguageTag(targetLang)
                    tts?.speak(translated, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    /**
     * Called when the user holds the Mic button
     */
    fun startVoiceRecording(speaker: SpeakerRole) {
        if (isCurrentlyOnline) {
            // ONLINE: Start streaming audio chunks
            if (!isGeminiSetupComplete) return
            currentSpeaker = speaker
            currentOriginalText = "(Voice input)"
            currentGeminiText = ""

            viewModelScope.launch(Dispatchers.IO) {
                audioRecorder.startRecording().collect { chunk ->
                    val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
                    val msg = JSONObject().apply {
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=16000")
                                    put("data", base64Data)
                                })
                            })
                        })
                    }
                    webSocket?.send(msg.toString())
                }
            }
        } else {
            // OFFLINE: Trigger Android's built-in Intent.ACTION_RECOGNIZE_SPEECH
        }
    }

    fun stopVoiceRecording() {
        audioRecorder.stopRecording()
        if (isCurrentlyOnline) {
            // Tell Gemini we are done speaking so it can start translating
            val endTurn = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turnComplete", true)
                })
            }
            webSocket?.send(endTurn.toString())
        }
    }

    fun playTranslation(text: String, language: String) {
        tts?.language = Locale.forLanguageTag(language)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            audioPlayer.stop()
            tts?.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        audioRecorder.stopRecording()
        audioPlayer.stop()
        webSocket?.close(1000, "User left screen")
    }
}