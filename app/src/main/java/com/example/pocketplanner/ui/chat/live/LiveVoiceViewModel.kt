package com.example.pocketplanner.ui.chat.live

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

enum class LiveVoiceState {
    IDLE, CONNECTING, LISTENING, SPEAKING, ERROR
}

class LiveVoiceViewModel : ViewModel() {

    private val _state = MutableStateFlow(LiveVoiceState.IDLE)
    val state: StateFlow<LiveVoiceState> = _state

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff

    fun toggleCamera() {
        _isCameraOff.value = !_isCameraOff.value
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
        .build()
    private var webSocket: WebSocket? = null
    
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer()

    private val projectId = "pocketplanner-b9422"
    private val region = "us-central1"
    private val model = "gemini-live-2.5-flash-native-audio"
    private val endpointUrl = "wss://$region-aiplatform.googleapis.com/ws/google.cloud.aiplatform.v1.LlmBidiService/BidiGenerateContent"

    private var isSetupComplete = false
    private var appContext: android.content.Context? = null

    /** Must be called from the UI to pass the application context */
    fun setContext(context: android.content.Context) {
        appContext = context.applicationContext
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startCall() {
        if (_state.value != LiveVoiceState.IDLE && _state.value != LiveVoiceState.ERROR) return
        
        _state.value = LiveVoiceState.CONNECTING
        _errorMessage.value = null
        isSetupComplete = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Generate OAuth2 access token from Service Account
                val accessToken = generateAccessToken()
                Log.d("LiveVoice", "Got access token: ${accessToken.take(20)}...")
                
                // Init audio player
                withContext(Dispatchers.Main) {
                    audioPlayer.init()
                }

                // Start a timeout watchdog
                viewModelScope.launch {
                    kotlinx.coroutines.delay(60_000)
                    if (_state.value == LiveVoiceState.CONNECTING) {
                        _errorMessage.value = "Connection timed out. Check your network and try again."
                        _state.value = LiveVoiceState.ERROR
                        endCall()
                    }
                }

                val request = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d("LiveVoice", "WebSocket Opened")
                        
                        val setupMsg = JSONObject().apply {
                            put("setup", JSONObject().apply {
                                put("model", "projects/$projectId/locations/$region/publishers/google/models/$model")
                                put("generationConfig", JSONObject().apply {
                                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                                })
                            })
                        }
                        // Android's JSONObject escapes forward slashes by default (e.g., projects\/pocketplanner)
                        // This breaks the Vertex AI parser. We must unescape them.
                        val setupJsonStr = setupMsg.toString().replace("\\/", "/")
                        Log.d("LiveVoice", "Sending Setup: $setupJsonStr")
                        webSocket.send(setupJsonStr)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                        Log.d("LiveVoice", "Received Binary Frame: ${bytes.size} bytes")
                        // Many gRPC-Web gateways send JSON text inside binary frames!
                        val text = bytes.utf8()
                        onMessage(webSocket, text)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            Log.d("LiveVoice", "Received JSON Keys: ${json.keys().asSequence().toList()}")
                            
                            if (json.has("error")) {
                                val errorObj = json.getJSONObject("error")
                                val msg = errorObj.optString("message", "Unknown server error")
                                Log.e("LiveVoice", "Server error: $msg")
                                _errorMessage.value = msg
                                
                                isSetupComplete = false
                                audioRecorder.stopRecording()
                                audioPlayer.stop()
                                
                                _state.value = LiveVoiceState.ERROR
                                return
                            }
                            
                            if (json.has("setupComplete")) {
                                isSetupComplete = true
                                _state.value = LiveVoiceState.LISTENING
                                startRecordingAndStreaming()
                            }
                            
                            if (json.has("serverContent")) {
                                val serverContent = json.getJSONObject("serverContent")
                                if (serverContent.has("modelTurn")) {
                                    _state.value = LiveVoiceState.SPEAKING
                                    val parts = serverContent.getJSONObject("modelTurn").getJSONArray("parts")
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        if (part.has("inlineData")) {
                                            val data = part.getJSONObject("inlineData").getString("data")
                                            val pcmBytes = Base64.decode(data, Base64.DEFAULT)
                                            audioPlayer.play(pcmBytes)
                                        }
                                    }
                                }
                                if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                                    _state.value = LiveVoiceState.LISTENING
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LiveVoice", "Error parsing message", e)
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        val errorBody = try { response?.body?.string() } catch (e: Exception) { null }
                        Log.e("LiveVoice", "WebSocket Failure: ${t.message}. Body: $errorBody", t)
                        _errorMessage.value = errorBody ?: t.localizedMessage ?: "Connection failed"
                        
                        isSetupComplete = false
                        audioRecorder.stopRecording()
                        audioPlayer.stop()
                        
                        _state.value = LiveVoiceState.ERROR
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d("LiveVoice", "WebSocket Closed: code=$code reason='$reason'")
                        
                        isSetupComplete = false
                        audioRecorder.stopRecording()
                        audioPlayer.stop()

                        if (_state.value == LiveVoiceState.CONNECTING || _state.value == LiveVoiceState.LISTENING || _state.value == LiveVoiceState.SPEAKING) {
                            // Unexpected close — show error
                            _errorMessage.value = if (reason.isNotBlank()) reason else "Server closed connection (code: $code). Model may not be available."
                            _state.value = LiveVoiceState.ERROR
                        } else if (reason.isNotBlank() && reason != "User ended call") {
                            _errorMessage.value = reason
                            _state.value = LiveVoiceState.ERROR
                        } else {
                            _state.value = LiveVoiceState.IDLE
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("LiveVoice", "Failed to start call", e)
                _errorMessage.value = e.localizedMessage ?: "Failed to authenticate"
                _state.value = LiveVoiceState.ERROR
            }
        }
    }

    private suspend fun generateAccessToken(): String {
        try {
            val functions = Firebase.functions

            // Call the backend function we just deployed
            val result = functions
                .getHttpsCallable("getVertexAccessToken")
                .call()
                .await() // Suspends the coroutine until the network request finishes

            val data = result.data as? Map<String, Any>
            val token = data?.get("token") as? String

            return token ?: throw Exception("Token missing in server response")

        } catch (e: Exception) {
            Log.e("LiveVoice", "Failed to fetch Vertex token from Firebase Functions", e)
            throw e
        }
    }

    private fun startRecordingAndStreaming() {
        viewModelScope.launch(Dispatchers.IO) {
            audioRecorder.startRecording().collect { chunk ->
                if (isSetupComplete && !_isMuted.value && (_state.value == LiveVoiceState.LISTENING || _state.value == LiveVoiceState.SPEAKING)) {
                    val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
                    
                    val realtimeInput = JSONObject().apply {
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=16000")
                                    put("data", base64Data)
                                })
                            })
                        })
                    }
                    webSocket?.send(realtimeInput.toString())
                }
            }
        }
    }

    fun sendVideoFrame(jpegBytes: ByteArray) {
        if (!isSetupComplete || _isCameraOff.value) return
        val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val realtimeInput = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            })
        }
        webSocket?.send(realtimeInput.toString())
    }

    fun endCall() {
        _state.value = LiveVoiceState.IDLE
        isSetupComplete = false
        audioRecorder.stopRecording()
        audioPlayer.stop()
        webSocket?.close(1000, "User ended call")
        webSocket = null
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
    }
}
