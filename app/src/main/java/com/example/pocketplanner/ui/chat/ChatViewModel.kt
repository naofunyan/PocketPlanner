package com.example.pocketplanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val translationManager: TranslationManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var userLanguage: String = "en"

    // --- VERTEX AI REST API CONFIG ---
    private val apiKey = BuildConfig.VERTEX_API_KEY
    private val projectId = "pocketplanner-b9422"
    private val region = "us-central1"
    private val endpointUrl = "https://$region-aiplatform.googleapis.com/v1/projects/$projectId/locations/$region/publishers/google/models/gemini-2.5-flash:generateContent?key=$apiKey"

    // To maintain chat history for the REST API
    private val chatHistory = mutableListOf<JSONObject>()

    init {
        // System prompt / context
        chatHistory.add(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "You are PocketPlanner, a friendly travel assistant for Vietnam. Keep your answers helpful and concise.")
                })
            })
        })
        chatHistory.add(JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "Understood. I am PocketPlanner, your friendly travel assistant!")
                })
            })
        })

        _messages.value = listOf(
            ChatMessage(text = "Hello! I am PocketPlanner. How can I help you plan your trip today?", isFromUser = false)
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(text = userText, isFromUser = true)
        val loadingMessage = ChatMessage(text = "AI is typing...", isFromUser = false, isLoading = true)

        _messages.value = _messages.value + userMessage + loadingMessage

        viewModelScope.launch {
            try {
                // 1. Detect language
                val detectedLang = translationManager.detectLanguage(userText)
                if (detectedLang != null) {
                    userLanguage = detectedLang
                }

                // 2. Translate to English for Gemini
                val englishPrompt = if (userLanguage != "en") {
                    translationManager.translate(userText, userLanguage, "en")
                } else {
                    userText
                }

                // Append to REST chat history
                chatHistory.add(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", englishPrompt)
                        })
                    })
                })

                // 3. Ask Vertex AI via REST
                val aiResponseText = generateWithRest()

                // Append AI response to history
                chatHistory.add(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", aiResponseText)
                        })
                    })
                })

                // 4. Translate back
                var finalResponse = aiResponseText
                if (userLanguage != "en") {
                    finalResponse = translationManager.translate(finalResponse, "en", userLanguage)
                }

                // 5. Update UI
                _messages.value = _messages.value.dropLast(1) + ChatMessage(text = finalResponse, isFromUser = false)

            } catch (e: Exception) {
                // Remove the failed user prompt from history so we don't break the alternate user/model flow
                if (chatHistory.isNotEmpty() && chatHistory.last().getString("role") == "user") {
                    chatHistory.removeAt(chatHistory.size - 1)
                }
                _messages.value = _messages.value.dropLast(1) + ChatMessage(text = "Error: ${e.localizedMessage}", isFromUser = false)
            }
        }
    }

    private suspend fun generateWithRest(): String = withContext(Dispatchers.IO) {
        val url = URL(endpointUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val requestBody = JSONObject().apply {
            put("contents", JSONArray(chatHistory))
        }

        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

        if (connection.responseCode == 200) {
            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)
            return@withContext jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            throw Exception("HTTP ${connection.responseCode}: $errorResponse")
        }
    }
}