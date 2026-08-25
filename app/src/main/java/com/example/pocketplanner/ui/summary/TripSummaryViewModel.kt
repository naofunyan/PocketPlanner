package com.example.pocketplanner.ui.summary

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.BuildConfig
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.ExpenseRepository
import com.example.pocketplanner.data.repository.TripRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class TripSummaryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _trip = MutableStateFlow<TripEntity?>(null)
    val trip: StateFlow<TripEntity?> = _trip

    private val _places = MutableStateFlow<List<PlaceEntity>>(emptyList())
    val places: StateFlow<List<PlaceEntity>> = _places

    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntity>> = _expenses

    private val _isGeneratingDiary = MutableStateFlow(false)
    val isGeneratingDiary: StateFlow<Boolean> = _isGeneratingDiary

    fun loadTripSummary(tripId: String) {
        viewModelScope.launch {
            tripRepository.getTrip(tripId).collect { t ->
                _trip.value = t
            }
        }
        viewModelScope.launch {
            tripRepository.getAllPlacesForTrip(tripId).collect { p ->
                _places.value = p
            }
        }
        viewModelScope.launch {
            expenseRepository.getExpenses(tripId).collect { e ->
                _expenses.value = e
            }
        }
    }

    fun ratePlace(place: PlaceEntity, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.savePlaces(listOf(place.copy(userRating = rating)))
        }
    }

    fun publishTrip(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val currentTrip = _trip.value ?: return
        val tripPlaces = _places.value
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val shareId = currentTrip.shareId ?: UUID.randomUUID().toString().substring(0, 8).uppercase()
                
                val tripMap = mapOf(
                    "name" to currentTrip.name,
                    "destination" to currentTrip.destination,
                    "durationDays" to if (currentTrip.isOpenEnded) ((System.currentTimeMillis() - currentTrip.startDate) / 86400000L).toInt().coerceAtLeast(1) else ((currentTrip.endDate - currentTrip.startDate) / 86400000L).toInt().coerceAtLeast(1),
                    "aiDiary" to (currentTrip.aiDiary ?: "")
                )
                
                val placesList = tripPlaces.map { p ->
                    mapOf(
                        "name" to p.name,
                        "category" to p.category,
                        "lat" to p.lat,
                        "lng" to p.lng,
                        "dayNumber" to p.dayNumber,
                        "userRating" to (p.userRating ?: 0)
                    )
                }
                
                val sharedDoc = mapOf(
                    "trip" to tripMap,
                    "places" to placesList,
                    "publishedAt" to System.currentTimeMillis()
                )
                
                db.collection("shared_trips").document(shareId).set(sharedDoc).await()
                
                val webLink = "https://pocketplanner.app/trip/$shareId"
                
                // Update local trip
                val updatedTrip = currentTrip.copy(isPublished = true, shareId = shareId)
                tripRepository.updateTrip(updatedTrip)
                
                withContext(Dispatchers.Main) {
                    onSuccess(webLink)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to publish trip")
                }
            }
        }
    }

    fun generateDiary(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        android.util.Log.d("TripSummary", "generateDiary called!")
        val currentTrip = _trip.value
        if (currentTrip == null) {
            onError("Trip not loaded")
            return
        }
        if (!currentTrip.aiDiary.isNullOrBlank()) {
            android.util.Log.d("TripSummary", "Already has aiDiary: ${currentTrip.aiDiary}")
            return // Already has one
        }
        val tripPlaces = _places.value
        val tripExpenses = _expenses.value
        
        _isGeneratingDiary.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalSpent = tripExpenses.sumOf { it.convertedAmountVND }
                val placesSummary = tripPlaces.take(15).joinToString(", ") { it.name }
                
                val prompt = """
                    Write a highly engaging, nostalgic 2-paragraph travel diary summary for a trip to ${currentTrip.destination}.
                    The traveler visited these places: $placesSummary.
                    They spent a total of roughly $totalSpent VND.
                    Focus on the experiences and memories, making it sound like a personal travel blog. No markdown formatting.
                """.trimIndent()
                
                val vertexApiKey = BuildConfig.VERTEX_API_KEY
                val aiEndpoint = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$vertexApiKey"
                
                val payload = JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }
                
                val url = URL(aiEndpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                java.io.OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                }
                
                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(responseText)
                    val text = responseJson.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""
                        
                    if (text.isNotBlank()) {
                        val updatedTrip = currentTrip.copy(aiDiary = text.trim())
                        tripRepository.updateTrip(updatedTrip)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        withContext(Dispatchers.Main) { onError("Empty AI response") }
                    }
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.readText()
                    android.util.Log.e("TripSummary", "Gemini API Error: ${connection.responseCode} - $errorText")
                    withContext(Dispatchers.Main) { onError("API Error ${connection.responseCode}: $errorText") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("TripSummary", "Exception in generateDiary: ${e.message}")
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            } finally {
                _isGeneratingDiary.value = false
            }
        }
    }

    fun exportExpensesToCsv(context: Context): Uri? {
        val currentTrip = _trip.value ?: return null
        val tripExpenses = _expenses.value
        
        return try {
            val file = File(context.cacheDir, "expenses_${currentTrip.id}.csv")
            FileOutputStream(file).use { fos ->
                val writer = fos.writer()
                writer.write("Date,Category,Description,Amount,Currency,ConvertedAmount(VND)\n")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                tripExpenses.forEach { e ->
                    val dateStr = sdf.format(Date(e.date))
                    val desc = e.description.replace(",", " ") // prevent csv break
                    writer.write("${dateStr},${e.category},${desc},${e.amount},VND,${e.convertedAmountVND}\n")
                }
                writer.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
