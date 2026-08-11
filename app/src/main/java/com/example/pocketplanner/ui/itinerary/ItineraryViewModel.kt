package com.example.pocketplanner.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.TripRepository
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
import java.util.UUID
import javax.inject.Inject

import com.example.pocketplanner.BuildConfig

@HiltViewModel
class ItineraryViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    // --- VERTEX AI REST API CONFIG ---
    // Securely reading the API Key from local.properties -> BuildConfig
    private val apiKey = BuildConfig.VERTEX_API_KEY
    // Your Google Cloud Project ID
    private val projectId = "pocketplanner-b9422"
    // The region of your Google Cloud Project (usually us-central1)
    private val region = "us-central1"
    
    private val endpointUrl = "https://$region-aiplatform.googleapis.com/v1/projects/$projectId/locations/$region/publishers/google/models/gemini-2.5-flash:generateContent?key=$apiKey"

    fun loadTrips(userId: String) {
        viewModelScope.launch {
            tripRepository.getAllTrips(userId).collect { tripList ->
                _trips.value = tripList
            }
        }
    }

    fun generateTripWithAI(userId: String, destination: String, days: Int) {
        viewModelScope.launch {
            try {
                val prompt = "Plan a $days day trip to $destination. Give me a 2 sentence summary of what the vibe will be."
                
                // Call Vertex AI REST API directly (Bypasses Firebase & AI Studio completely!)
                val aiResponseText = generateWithRest(prompt)

                val newTrip = TripEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    destination = destination,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (days * 86400000L),
                    budget = 5000000.0,
                    status = "UPCOMING"
                )
                tripRepository.createTrip(newTrip)

                println("Gemini Says: $aiResponseText")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun generateWithRest(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(endpointUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        // Build the JSON payload exactly how Vertex AI expects it
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        // Send the request
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

        // Read the response
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