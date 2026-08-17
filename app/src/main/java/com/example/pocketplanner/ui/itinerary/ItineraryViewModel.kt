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
import com.example.pocketplanner.data.local.entity.PlaceEntity

import com.example.pocketplanner.core.location.GeofenceManager
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class ItineraryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _coverPhotoUrl = MutableStateFlow<String?>(null)
    val coverPhotoUrl: StateFlow<String?> = _coverPhotoUrl.asStateFlow()

    // Map of Currency Code (e.g. "USD") to its rate relative to USD
    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    // --- VERTEX AI REST API CONFIG ---
    // Securely reading the API Key from local.properties -> BuildConfig
    private val apiKey = BuildConfig.VERTEX_API_KEY
    // Your Google Cloud Project ID
    private val projectId = "pocketplanner-b9422"
    // The region of your Google Cloud Project (usually us-central1)
    private val region = "us-central1"
    
    private val endpointUrl = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$apiKey"

    fun loadTrips(userId: String) {
        viewModelScope.launch {
            tripRepository.getAllTrips(userId).collect { tripList ->
                _trips.value = tripList
            }
        }
    }

    fun fetchInitialCoverPhoto(destination: String) {
        viewModelScope.launch {
            if (_coverPhotoUrl.value == null) {
                _coverPhotoUrl.value = fetchUnsplashPhoto(destination)
            }
        }
    }

    fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://open.er-api.com/v6/latest/USD")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("result") == "success") {
                        val ratesObj = jsonObject.getJSONObject("rates")
                        val ratesMap = mutableMapOf<String, Double>()
                        val keys = ratesObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            ratesMap[key] = ratesObj.getDouble(key)
                        }
                        _exchangeRates.value = ratesMap
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPlacesForDay(tripId: String, dayNumber: Int) = tripRepository.getPlacesForDay(tripId, dayNumber)
    fun getTrip(tripId: String) = tripRepository.getTrip(tripId)

    fun generateTripWithAI(
        userId: String, 
        destination: String, 
        days: Int, 
        name: String, 
        startDate: Long, 
        endDate: Long, 
        customPhotoUrl: String? = null,
        budgetAmount: Double? = null,
        budgetCurrency: String? = null,
        isTrackerEnabled: Boolean = false,
        trackingMode: String = "Balanced",
        onSuccess: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                // 1. Build prompt
                var prompt = """
                    You are a professional travel planner. Create a $days day itinerary for a trip to "$destination".
                    The user's trip is named "$name".
                """.trimIndent()
                
                if (budgetAmount != null && budgetCurrency != null) {
                    prompt += "\nThe user's budget for this trip is $budgetAmount $budgetCurrency. Please optimize the estimated costs for places accordingly."
                }
                
                prompt += """
                    
                    Respond strictly in this JSON format:

                    {
                      "days": [
                        {
                          "dayNumber": 1,
                          "places": [
                            {
                              "name": "Eiffel Tower",
                              "lat": 48.8584,
                              "lng": 2.2945,
                              "category": "Sightseeing",
                              "estimatedCost": 25.0,
                              "notes": "Book tickets in advance."
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()

                // 2. Call Gemini
                val aiResponseText = generateWithRest(prompt)

                // 3. Clean the response (Gemini sometimes wraps JSON in markdown blocks)
                val cleanJson = aiResponseText.removePrefix("```json").removeSuffix("```").trim()

                // 4. Fetch Unsplash Photo
                val photoUrl = customPhotoUrl ?: _coverPhotoUrl.value ?: fetchUnsplashPhoto(destination)

                // 5. Create the Trip
                val tripId = UUID.randomUUID().toString()
                
                // Convert budget to VND for storage if provided
                var budgetInVnd = 5000000.0 // Default 5M VND
                if (budgetAmount != null && budgetCurrency != null) {
                    val rates = _exchangeRates.value
                    if (rates.isNotEmpty()) {
                        val rateVnd = rates["VND"] ?: 25000.0
                        val rateCurrency = rates[budgetCurrency] ?: 1.0
                        budgetInVnd = budgetAmount * (rateVnd / rateCurrency)
                    }
                }
                
                val newTrip = TripEntity(
                    id = tripId,
                    userId = userId,
                    name = name,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    budget = budgetInVnd,
                    status = "UPCOMING",
                    photoUrl = photoUrl,
                    isTrackerEnabled = isTrackerEnabled,
                    trackingMode = trackingMode
                )
                tripRepository.createTrip(newTrip)

                // 5. Parse the JSON and save Places
                val jsonObject = JSONObject(cleanJson)
                val daysArray = jsonObject.getJSONArray("days")

                val placesList = mutableListOf<PlaceEntity>()

                for (i in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(i)
                    val dayNum = dayObj.getInt("dayNumber")
                    val placesArray = dayObj.getJSONArray("places")

                    for (j in 0 until placesArray.length()) {
                        val placeObj = placesArray.getJSONObject(j)
                        placesList.add(
                            PlaceEntity(
                                id = UUID.randomUUID().toString(),
                                tripId = tripId,
                                dayNumber = dayNum,
                                name = placeObj.getString("name"),
                                lat = placeObj.getDouble("lat"),
                                lng = placeObj.getDouble("lng"),
                                category = placeObj.getString("category"),
                                estimatedCost = placeObj.optDouble("estimatedCost", 0.0),
                                notes = placeObj.optString("notes", "")
                            )
                        )
                    }
                }

                // Save to database
                tripRepository.savePlaces(placesList)

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error occurred")
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.deleteTrip(tripId)
        }
    }

    fun updateTripSettings(
        tripId: String,
        newName: String,
        newStart: Long,
        newEnd: Long,
        newBudgetAmount: Double?,
        newBudgetCurrency: String?,
        isTrackerEnabled: Boolean,
        trackingMode: String,
        newPhotoUrl: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val oldTrip = tripRepository.getTrip(tripId).firstOrNull()
                if (oldTrip == null) {
                    withContext(Dispatchers.Main) { onError("Trip not found") }
                    return@launch
                }
                
                var budgetInVnd = oldTrip.budget
                if (newBudgetAmount != null && newBudgetCurrency != null) {
                    val rates = _exchangeRates.value
                    if (rates.isNotEmpty()) {
                        val rateVnd = rates["VND"] ?: 25000.0
                        val rateCurrency = rates[newBudgetCurrency] ?: 1.0
                        budgetInVnd = newBudgetAmount * (rateVnd / rateCurrency)
                    }
                }
                
                val oldTotalDays = ((oldTrip.endDate - oldTrip.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                val newTotalDays = ((newEnd - newStart) / 86400000L).toInt().coerceAtLeast(0) + 1
                val budgetChanged = kotlin.math.abs(budgetInVnd - oldTrip.budget) > 1.0
                
                if (newTotalDays > oldTotalDays || budgetChanged) {
                    _isGenerating.value = true
                }
                
                if (newTotalDays < oldTotalDays) {
                    tripRepository.deletePlacesForDaysGreaterThan(tripId, newTotalDays)
                } else if (newTotalDays > oldTotalDays) {
                    // Extend itinerary
                    val existingPlaces = tripRepository.getAllPlacesForTrip(tripId).firstOrNull() ?: emptyList()
                    val visitedPlacesStr = if (existingPlaces.isNotEmpty()) {
                        existingPlaces.joinToString(", ") { it.name }
                    } else {
                        "none"
                    }
                    
                    val addedDays = newTotalDays - oldTotalDays
                    var prompt = """
                        The user extended their trip named "${oldTrip.name}" to "${oldTrip.destination}" by $addedDays days.
                        They have already planned to visit or visited: $visitedPlacesStr.
                        Please plan an itinerary for the additional days (day ${oldTotalDays + 1} to $newTotalDays).
                        Ensure you DO NOT recommend any of the places they have already planned or visited.
                    """.trimIndent()
                    
                    if (newBudgetAmount != null && newBudgetCurrency != null) {
                        prompt += "\nThe user's budget for this trip is $newBudgetAmount $newBudgetCurrency. Please optimize the estimated costs for places accordingly."
                    }
                    
                    prompt += """
                        
                        Respond strictly in this JSON format:
    
                        {
                          "days": [
                            {
                              "dayNumber": 1,
                              "places": [
                                {
                                  "name": "Eiffel Tower",
                                  "lat": 48.8584,
                                  "lng": 2.2945,
                                  "category": "Sightseeing",
                                  "estimatedCost": 25.0,
                                  "notes": "Book tickets in advance."
                                }
                              ]
                            }
                          ]
                        }
                    """.trimIndent()
                    
                    val aiResponseText = generateWithRest(prompt)
                    val cleanJson = aiResponseText.removePrefix("```json").removeSuffix("```").trim()
                    
                    val jsonObject = JSONObject(cleanJson)
                    val daysArray = jsonObject.getJSONArray("days")
                    val placesList = mutableListOf<PlaceEntity>()
                    
                    for (i in 0 until daysArray.length()) {
                        val dayObj = daysArray.getJSONObject(i)
                        val dayNum = dayObj.getInt("dayNumber")
                        val placesArray = dayObj.getJSONArray("places")
                        
                        for (j in 0 until placesArray.length()) {
                            val placeObj = placesArray.getJSONObject(j)
                            placesList.add(
                                PlaceEntity(
                                    id = UUID.randomUUID().toString(),
                                    tripId = tripId,
                                    dayNumber = dayNum,
                                    name = placeObj.getString("name"),
                                    lat = placeObj.getDouble("lat"),
                                    lng = placeObj.getDouble("lng"),
                                    category = placeObj.getString("category"),
                                    estimatedCost = placeObj.optDouble("estimatedCost", 0.0),
                                    notes = placeObj.optString("notes", "")
                                )
                            )
                        }
                    }
                    tripRepository.savePlaces(placesList)
                }
                
                if (budgetChanged) {
                    val existingPlaces = tripRepository.getAllPlacesForTrip(tripId).firstOrNull() ?: emptyList()
                    if (existingPlaces.isNotEmpty()) {
                        val placesJsonArray = JSONArray()
                        existingPlaces.forEach { place ->
                            val obj = JSONObject()
                            obj.put("id", place.id)
                            obj.put("name", place.name)
                            obj.put("category", place.category)
                            obj.put("estimatedCost", place.estimatedCost)
                            obj.put("notes", place.notes)
                            placesJsonArray.put(obj)
                        }
                        
                        val prompt = """
                            The user has updated their budget for the trip "${oldTrip.name}" to $newBudgetAmount $newBudgetCurrency.
                            Here are the currently planned places:
                            ${placesJsonArray.toString()}
                            
                            Please update ONLY the `estimatedCost` and `notes` for these places to fit the new budget. 
                            You can adjust the costs or change the notes to suggest cheaper/more expensive options for these specific places.
                            Do NOT add or remove any places.
                            
                            Respond strictly with a JSON array of objects, where each object has exactly these keys:
                            "id" (must match the original id string), "estimatedCost" (number), and "notes" (string).
                        """.trimIndent()
                        
                        val aiResponseText = generateWithRest(prompt)
                        val cleanJson = aiResponseText.removePrefix("```json").removeSuffix("```").trim()
                        val updatedArray = JSONArray(cleanJson)
                        
                        val updatedPlaces = mutableListOf<PlaceEntity>()
                        for (i in 0 until updatedArray.length()) {
                            val updatedObj = updatedArray.getJSONObject(i)
                            val pId = updatedObj.getString("id")
                            val newCost = updatedObj.getDouble("estimatedCost")
                            val newNotes = updatedObj.getString("notes")
                            
                            existingPlaces.find { it.id == pId }?.let { originalPlace ->
                                updatedPlaces.add(originalPlace.copy(estimatedCost = newCost, notes = newNotes))
                            }
                        }
                        if (updatedPlaces.isNotEmpty()) {
                            tripRepository.savePlaces(updatedPlaces)
                        }
                    }
                }
                
                val updatedTrip = oldTrip.copy(
                    name = newName,
                    startDate = newStart,
                    endDate = newEnd,
                    budget = budgetInVnd,
                    isTrackerEnabled = isTrackerEnabled,
                    trackingMode = trackingMode,
                    photoUrl = newPhotoUrl ?: oldTrip.photoUrl
                )
                
                tripRepository.createTrip(updatedTrip)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError(e.message ?: "Update failed") }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun fetchUnsplashPhoto(destination: String): String? = withContext(Dispatchers.IO) {
        try {
            val unsplashKey = BuildConfig.UNSPLASH_API_KEY
            if (unsplashKey.isBlank()) return@withContext null
            
            // Encode destination for URL
            val query = java.net.URLEncoder.encode(destination, "UTF-8")
            val url = URL("https://api.unsplash.com/search/photos?query=${query}&per_page=1&client_id=$unsplashKey")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseString)
                val resultsArray = jsonObject.optJSONArray("results")
                if (resultsArray != null && resultsArray.length() > 0) {
                    val firstResult = resultsArray.getJSONObject(0)
                    val urlsObj = firstResult.getJSONObject("urls")
                    return@withContext urlsObj.getString("regular")
                }
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun enableProximityAlerts(tripId: String) {
        viewModelScope.launch {
            val days = (1..10)
            val allPlaces = mutableListOf<com.example.pocketplanner.data.local.entity.PlaceEntity>()
            
            for (day in days) {
                val placesForDay = tripRepository.getPlacesForDay(tripId, day).firstOrNull()
                if (placesForDay != null) {
                    allPlaces.addAll(placesForDay)
                }
            }
            
            if (allPlaces.isNotEmpty()) {
                geofenceManager.addGeofencesForPlaces(allPlaces)
            }
        }
    }

    private suspend fun generateWithRest(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(endpointUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 60000 // Increased to 60 seconds for large JSON responses

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

    // --- WEATHER API LOGIC ---
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    fun fetchWeather(context: android.content.Context, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                // 1. Get City Name using Geocoder
                val city = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        addresses?.firstOrNull()?.let { address ->
                            address.locality ?: address.subAdminArea ?: address.adminArea
                        } ?: "N/A"
                    } catch (e: Exception) {
                        "N/A"
                    }
                }

                // 2. Fetch from Open-Meteo (No API Key Required)
                val weatherData = withContext(Dispatchers.IO) {
                    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current_weather=true")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode == 200) {
                        val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(responseString).getJSONObject("current_weather")
                    } else {
                        throw Exception("Weather API failed")
                    }
                }

                val temp = weatherData.getDouble("temperature")
                val code = weatherData.getInt("weathercode")
                
                // Map WMO Weather codes to descriptions
                val description = when (code) {
                    0 -> "clear skies"
                    1, 2, 3 -> "partly cloudy"
                    45, 48 -> "foggy"
                    51, 53, 55 -> "drizzling"
                    61, 63, 65 -> "raining"
                    71, 73, 75 -> "snowing"
                    95, 96, 99 -> "thunderstorms"
                    else -> "variable"
                }

                _weatherState.value = WeatherState.Success(city, temp, description)
            } catch (e: Exception) {
                e.printStackTrace()
                _weatherState.value = WeatherState.Error
            }
        }
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val city: String, val temperature: Double, val description: String) : WeatherState()
    object Error : WeatherState()
}