package com.example.pocketplanner.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.ExpenseRepository
import com.example.pocketplanner.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.pocketplanner.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    private val _isGlobalMode = MutableStateFlow(false)
    val isGlobalMode: StateFlow<Boolean> = _isGlobalMode.asStateFlow()

    private val _allTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val allTrips: StateFlow<List<TripEntity>> = _allTrips.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    init {
        fetchExchangeRates()
    }

    private fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Free, no-key CDN API for daily exchange rates
                val response = URL("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json").readText()
                val json = JSONObject(response)
                val usdRates = json.getJSONObject("usd")

                // Build a map of the rates we care about
                val ratesMap = mutableMapOf<String, Double>()
                ratesMap["VND"] = usdRates.getDouble("vnd")
                ratesMap["USD"] = 1.0
                // You can add "EUR" or others here later if needed!

                _exchangeRates.value = ratesMap
            } catch (e: Exception) {
                e.printStackTrace()
                // If offline, fallback to a standard rate so the app doesn't break
                _exchangeRates.value = mapOf("VND" to 25000.0, "USD" to 1.0)
            }
        }
    }

    fun initialize(initialTripId: String, userId: String) {
        if (initialTripId.isNotEmpty()) {
            // TRIP MODE: Lock to the specific trip passed in
            _isGlobalMode.value = false
            _selectedTripId.value = initialTripId
        } else {
            // GLOBAL MODE: Fetch all trips and apply default selection logic
            _isGlobalMode.value = true
            viewModelScope.launch {
                tripRepository.getAllTrips(userId).collect { trips ->
                    _allTrips.value = trips

                    // Only auto-select if the user hasn't manually picked one yet
                    if (_selectedTripId.value == null && trips.isNotEmpty()) {
                        val now = System.currentTimeMillis()

                        // 1. Currently active trip
                        val activeTrip = trips.firstOrNull { it.startDate <= now && (it.isOpenEnded || it.endDate >= now) }
                        // 2. Closest upcoming trip
                        val upcomingTrip = trips.filter { it.startDate > now }.minByOrNull { it.startDate }

                        _selectedTripId.value = (activeTrip ?: upcomingTrip)?.id
                    }
                }
            }
        }
    }

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    // Reactively swap streams when selectedTripId changes
    val currentTrip: Flow<TripEntity?> = _selectedTripId.flatMapLatest { id ->
        if (id == null) flowOf(null) else tripRepository.getTrip(id)
    }

    val expenses: Flow<List<ExpenseEntity>> = _selectedTripId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else expenseRepository.getExpenses(id)
    }

    val budgetOverview: Flow<BudgetState> = _selectedTripId.flatMapLatest { id ->
        if (id == null) {
            flowOf(BudgetState(0.0, 0.0, 0.0))
        } else {
            combine(
                tripRepository.getTrip(id),
                expenseRepository.getTotalSpent(id)
            ) { trip, totalSpent ->
                BudgetState(
                    totalBudget = trip?.budget ?: 0.0,
                    totalSpent = totalSpent ?: 0.0,
                    remaining = (trip?.budget ?: 0.0) - (totalSpent ?: 0.0)
                )
            }
        }
    }

    // Add "date: Long = System.currentTimeMillis()" to the parameters
    fun addExpense(category: String, amount: Double, description: String, date: Long = System.currentTimeMillis()) {
        val tripId = _selectedTripId.value ?: return
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                category = category,
                amount = amount,
                convertedAmountVND = amount,
                description = description,
                date = date // <-- Use the passed date here!
            )
            expenseRepository.addExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    suspend fun scanReceipt(uri: Uri, context: Context): ScannedReceipt? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val maxDim = 1024
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            
            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            bitmap.recycle()

            val vertexApiKey = BuildConfig.VERTEX_API_KEY
            val aiEndpoint = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$vertexApiKey"
            
            val prompt = """
                You are an AI expense tracker. Analyze this receipt and extract the details.
                Return ONLY a JSON object matching this exact schema, without any markdown formatting:
                {
                  "amount": 12.50,
                  "currency": "VND" or "USD",
                  "category": "Food", // must be one of: Food, Transport, Lodging, Shopping, Activities, Bills, Groceries, Books, Gaming, Other
                  "notes": "Starbucks" // Merchant name or brief description
                }
            """.trimIndent()
            
            val payload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }
            
            val url = URL(aiEndpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().readText()
                val responseJson = JSONObject(responseText)
                
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        var text = parts.getJSONObject(0).optString("text")
                        
                        if (text.startsWith("```json")) {
                            text = text.substringAfter("```json").substringBeforeLast("```").trim()
                        } else if (text.startsWith("```")) {
                            text = text.substringAfter("```").substringBeforeLast("```").trim()
                        }
                        
                        val extracted = JSONObject(text)
                        return@withContext ScannedReceipt(
                            amount = extracted.optDouble("amount", 0.0),
                            currency = extracted.optString("currency", "VND"),
                            category = extracted.optString("category", "Other"),
                            notes = extracted.optString("notes", "")
                        )
                    }
                }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.readText()
                android.util.Log.e("ExpenseViewModel", "API Error: ${connection.responseCode} - $errorText")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}

data class BudgetState(val totalBudget: Double, val totalSpent: Double, val remaining: Double)

data class ScannedReceipt(
    val amount: Double,
    val currency: String,
    val category: String,
    val notes: String
)