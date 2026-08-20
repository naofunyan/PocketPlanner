package com.example.pocketplanner.ui.chat.translate

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.Locale

@HiltViewModel
class TranslateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // --- VERTEX AI REST API CONFIG ---
    private val apiKey = BuildConfig.VERTEX_API_KEY
    private val endpointUrl = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$apiKey"

    fun setSourceLanguage(lang: String) {
        _uiState.update { state ->
            val newTarget = if (lang == state.targetLanguage && lang != "auto") {
                if (lang == "en") "vi" else "en"
            } else {
                state.targetLanguage
            }
            state.copy(sourceLanguage = lang, targetLanguage = newTarget)
        }
        _uiState.value.capturedImage?.let { processBitmap(it) }
    }

    fun setTargetLanguage(lang: String) {
        _uiState.update { state ->
            val newSource = if (lang == state.sourceLanguage) {
                if (lang == "en") "vi" else "en"
            } else {
                state.sourceLanguage
            }
            state.copy(sourceLanguage = newSource, targetLanguage = lang)
        }
        // If we are looking at a frozen image, re-translate it with the new language!
        _uiState.value.capturedImage?.let { processBitmap(it) }
    }

    fun switchMode(mode: TranslateMode, bitmap: Bitmap? = null) {
        _uiState.update {
            it.copy(
                mode = mode,
                capturedImage = bitmap,
                blocks = if (mode == TranslateMode.LIVE) emptyList() else it.blocks
            )
        }
    }

    fun processBitmap(bitmap: Bitmap) {
        _uiState.update { it.copy(isProcessing = true, sourceImageWidth = bitmap.width, sourceImageHeight = bitmap.height) }
        val image = InputImage.fromBitmap(bitmap, 0)
        runOcrAndTranslate(image) {
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private fun runOcrAndTranslate(image: InputImage, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val visionText = recognizer.process(image).await()

                val originalBlocks = visionText.textBlocks.filter { it.text.isNotBlank() }
                if (originalBlocks.isEmpty()) {
                    _uiState.update { it.copy(blocks = emptyList()) }
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }

                // 1. Prepare texts for Gemini
                val textsToTranslate = JSONArray()
                originalBlocks.forEach { block ->
                    textsToTranslate.put(block.text.replace("\n", " "))
                }

                val targetLang = _uiState.value.targetLanguage
                val sourceLang = _uiState.value.sourceLanguage
                val translationDirection = if (sourceLang == "auto") {
                    "into $targetLang"
                } else {
                    "from $sourceLang to $targetLang"
                }
                
                val prompt = """
                    You are a professional translator. Translate the following JSON array of strings $translationDirection. 
                    Ensure context is preserved across the blocks (they are text blocks detected on a single page).
                    You MUST return ONLY a valid JSON array of translated strings in the EXACT same order and EXACT same length. Do not include markdown formatting or extra text.
                    
                    $textsToTranslate
                """.trimIndent()

                // 2. Build Request
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                }

                // 3. Ask Gemini 3.7 Flash via REST
                val url = URL(endpointUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

                if (connection.responseCode == 200) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseString)
                    val aiResponseText = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    var cleanText = aiResponseText.trim()
                    // Strip markdown blocks
                    if (cleanText.contains("```json")) {
                        cleanText = cleanText.substringAfter("```json").substringBeforeLast("```").trim()
                    } else if (cleanText.contains("```")) {
                        cleanText = cleanText.substringAfter("```").substringBeforeLast("```").trim()
                    }
                    // Strip leading/trailing conversational text by extracting just the array bracket bounds
                    if (cleanText.contains("[")) {
                        cleanText = cleanText.substring(cleanText.indexOf('['), cleanText.lastIndexOf(']') + 1)
                    }

                    val translatedArray = JSONArray(cleanText)
                    val detectedBlocks = mutableListOf<DetectedTextBlock>()

                    // 4. Map Translations back to Bounding Boxes
                    for (i in 0 until originalBlocks.size) {
                        val originalBlock = originalBlocks[i]
                        val originalText = originalBlock.text.replace("\n", " ")
                        val translatedText = if (i < translatedArray.length()) translatedArray.getString(i) else originalText

                        val cornerPoints = originalBlock.cornerPoints
                        var cornerX = 0f
                        var cornerY = 0f
                        var textWidth = 0f
                        var textHeight = 0f
                        var rotationAngle = 0f

                        if (cornerPoints != null && cornerPoints.size == 4) {
                            val p0 = cornerPoints[0]
                            val p1 = cornerPoints[1]
                            val p3 = cornerPoints[3]
                            
                            cornerX = p0.x.toFloat()
                            cornerY = p0.y.toFloat()
                            
                            textWidth = Math.hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
                            textHeight = Math.hypot((p3.x - p0.x).toDouble(), (p3.y - p0.y).toDouble()).toFloat()
                            
                            rotationAngle = Math.toDegrees(Math.atan2((p1.y - p0.y).toDouble(), (p1.x - p0.x).toDouble())).toFloat()
                        } else {
                            val rect = originalBlock.boundingBox
                            if (rect != null) {
                                cornerX = rect.left.toFloat()
                                cornerY = rect.top.toFloat()
                                textWidth = rect.width().toFloat()
                                textHeight = rect.height().toFloat()
                            }
                        }

                        detectedBlocks.add(
                            DetectedTextBlock(
                                originalText = originalText,
                                translatedText = translatedText,
                                boundingBox = originalBlock.boundingBox,
                                lineCount = originalBlock.lines.size.coerceAtLeast(1),
                                cornerX = cornerX,
                                cornerY = cornerY,
                                textWidth = textWidth,
                                textHeight = textHeight,
                                rotationAngle = rotationAngle
                            )
                        )
                    }
                    _uiState.update { it.copy(blocks = detectedBlocks) }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    throw Exception("HTTP ${connection.responseCode}: $errorResponse")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(blocks = emptyList()) }
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun getFullTranslatedText(): String {
        return _uiState.value.blocks.joinToString("\n\n") { it.translatedText }
    }

    private var tts: TextToSpeech? = null
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _uiState.update { it.copy(spokenBlockIndex = null, spokenLocalRange = null) }
                    }

                    override fun onDone(utteranceId: String?) {
                        _uiState.update { it.copy(spokenBlockIndex = null, spokenLocalRange = null) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _uiState.update { it.copy(spokenBlockIndex = null, spokenLocalRange = null) }
                    }
                    
                    // Requires API 26+
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        super.onRangeStart(utteranceId, start, end, frame)
                        
                        // Map the global (start, end) index back to the local block index and local character range!
                        val blocks = _uiState.value.blocks
                        var currentIndex = 0
                        
                        for (i in blocks.indices) {
                            val blockLength = blocks[i].translatedText.length
                            if (start >= currentIndex && start < currentIndex + blockLength) {
                                // Found the block!
                                val localStart = start - currentIndex
                                val localEnd = (end - currentIndex).coerceAtMost(blockLength)
                                _uiState.update { 
                                    it.copy(
                                        spokenBlockIndex = i,
                                        spokenLocalRange = IntRange(localStart, localEnd - 1)
                                    ) 
                                }
                                return
                            }
                            // +2 for the "\n\n" separator in joinToString
                            currentIndex += blockLength + 2
                        }
                    }
                })
            }
        }
    }

    fun speakAllText() {
        val fullText = getFullTranslatedText()
        if (fullText.isNotBlank()) {
            tts?.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "LENS_TTS")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _uiState.update { it.copy(spokenBlockIndex = null, spokenLocalRange = null) }
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
        tts?.stop()
        tts?.shutdown()
    }
}
