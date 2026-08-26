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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject

import com.example.pocketplanner.BuildConfig
import com.example.pocketplanner.R
import com.example.pocketplanner.data.local.entity.PlaceEntity

import com.example.pocketplanner.core.location.GeofenceManager
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class ItineraryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val geofenceManager: GeofenceManager,
    private val placeDetailsDao: com.example.pocketplanner.data.local.dao.PlaceDetailsDao,
    private val placeDao: com.example.pocketplanner.data.local.dao.PlaceDao
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

    private val _selectedPlaceDetails = MutableStateFlow<com.example.pocketplanner.data.local.entity.PlaceDetailsEntity?>(null)
    val selectedPlaceDetails: StateFlow<com.example.pocketplanner.data.local.entity.PlaceDetailsEntity?> = _selectedPlaceDetails.asStateFlow()

    private val _isFetchingPlaceDetails = MutableStateFlow(false)
    val isFetchingPlaceDetails: StateFlow<Boolean> = _isFetchingPlaceDetails.asStateFlow()

    data class RouteLeg(val walkDistance: Double, val walkDuration: Double, val driveDistance: Double, val driveDuration: Double, val geometry: String)

    private val _routeLegs = MutableStateFlow<Map<String, RouteLeg>>(emptyMap())
    val routeLegs: StateFlow<Map<String, RouteLeg>> = _routeLegs.asStateFlow()

    data class SearchResult(
        val fsqId: String,
        val name: String,
        val address: String,
        val lat: Double,
        val lng: Double,
        val category: String
    )

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun fetchRouteForDay(places: List<PlaceEntity>) {
        if (places.size < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if we already have the route for the first leg
                val key = "${places[0].id}_${places[1].id}"
                if (_routeLegs.value.containsKey(key)) return@launch
                
                val coordinates = places.joinToString(";") { "${it.lng},${it.lat}" }
                val token = com.example.pocketplanner.BuildConfig.MAPBOX_ACCESS_TOKEN
                
                // Fetch Walking
                val walkUrl = URL("https://api.mapbox.com/directions/v5/mapbox/walking/$coordinates?geometries=polyline&access_token=$token")
                val walkConn = walkUrl.openConnection() as HttpURLConnection
                walkConn.requestMethod = "GET"
                
                // Fetch Driving
                val driveUrl = URL("https://api.mapbox.com/directions/v5/mapbox/driving/$coordinates?geometries=polyline&access_token=$token")
                val driveConn = driveUrl.openConnection() as HttpURLConnection
                driveConn.requestMethod = "GET"
                
                if (walkConn.responseCode == HttpURLConnection.HTTP_OK && driveConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val walkResp = walkConn.inputStream.bufferedReader().use { it.readText() }
                    val driveResp = driveConn.inputStream.bufferedReader().use { it.readText() }
                    
                    val walkJson = org.json.JSONObject(walkResp)
                    val driveJson = org.json.JSONObject(driveResp)
                    
                    val walkRoutes = walkJson.getJSONArray("routes")
                    val driveRoutes = driveJson.getJSONArray("routes")
                    
                    if (walkRoutes.length() > 0 && driveRoutes.length() > 0) {
                        val walkRoute = walkRoutes.getJSONObject(0)
                        val driveRoute = driveRoutes.getJSONObject(0)
                        
                        val walkLegs = walkRoute.getJSONArray("legs")
                        val driveLegs = driveRoute.getJSONArray("legs")
                        
                        val newMap = _routeLegs.value.toMutableMap()
                        for (i in 0 until walkLegs.length()) {
                            val wLeg = walkLegs.getJSONObject(i)
                            val dLeg = driveLegs.getJSONObject(i)
                            
                            newMap["${places[i].id}_${places[i+1].id}"] = RouteLeg(
                                walkDistance = wLeg.getDouble("distance"),
                                walkDuration = wLeg.getDouble("duration"),
                                driveDistance = dLeg.getDouble("distance"),
                                driveDuration = dLeg.getDouble("duration"),
                                geometry = ""
                            )
                        }
                        
                        // Store the full route geometry (default to walking for polyline)
                        val fullGeometry = walkRoute.getString("geometry")
                        newMap["full_day_geometry_${places.first().dayNumber}"] = RouteLeg(0.0, 0.0, 0.0, 0.0, fullGeometry)
                        
                        _routeLegs.value = newMap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _placePhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val placePhotos: StateFlow<Map<String, String>> = _placePhotos.asStateFlow()

    fun fetchPhotoForPlace(place: PlaceEntity, destination: String) {
        // If already cached in the database, no need to fetch!
        if (place.photoUrl != null) {
            val currentMap = _placePhotos.value.toMutableMap()
            if (currentMap[place.id] != place.photoUrl) {
                currentMap[place.id] = place.photoUrl
                _placePhotos.value = currentMap
            }
            return
        }
        
        if (_placePhotos.value.containsKey(place.id)) return
        
        viewModelScope.launch(Dispatchers.IO) {
            var foundUrl: String? = null
            
            // 1. Foursquare API (Highly accurate using exact GPS coordinates)
            try {
                val fsqApiKey = BuildConfig.FOURSQUARE_API_KEY
                if (fsqApiKey.isNotBlank()) {
                    val queryUrl = "https://api.foursquare.com/v3/places/search?query=${java.net.URLEncoder.encode(place.name, "UTF-8")}&ll=${place.lat},${place.lng}&radius=2000&limit=1&fields=photos"
                    val url = URL(queryUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", fsqApiKey)
                    connection.setRequestProperty("Accept", "application/json")
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val root = org.json.JSONObject(response)
                        val results = root.getJSONArray("results")
                        if (results.length() > 0) {
                            val placeObj = results.getJSONObject(0)
                            val photosArray = placeObj.optJSONArray("photos")
                            if (photosArray != null && photosArray.length() > 0) {
                                val photoObj = photosArray.getJSONObject(0)
                                val prefix = photoObj.optString("prefix")
                                val suffix = photoObj.optString("suffix")
                                if (prefix.isNotEmpty() && suffix.isNotEmpty()) {
                                    foundUrl = "${prefix}500x500${suffix}"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 2. Unsplash API Fallback (Stock photo)
            if (foundUrl == null) {
                try {
                    val urls = fetchUnsplashPhotos("${place.category} ${place.name} $destination", 1)
                    if (urls.isNotEmpty()) foundUrl = urls.first()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (foundUrl != null) {
                val newMap = _placePhotos.value.toMutableMap()
                newMap[place.id] = foundUrl
                _placePhotos.value = newMap
                
                // Cache permanently into SQLite database!
                placeDao.updatePlace(place.copy(photoUrl = foundUrl))
            }
        }
    }

    // --- VERTEX AI REST API CONFIG ---
    // Securely reading the API Key from local.properties -> BuildConfig
    private val apiKey = BuildConfig.VERTEX_API_KEY
    // Your Google Cloud Project ID
    private val projectId = "pocketplanner-b9422"
    // The region of your Google Cloud Project (usually us-central1)
    private val region = "us-central1"
    
    private val endpointUrl = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$apiKey"

    fun clearSelectedPlaceDetails() {
        _selectedPlaceDetails.value = null
    }

    fun fetchPlaceDetails(place: PlaceEntity, destination: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingPlaceDetails.value = true
            try {
                // 1. Check local cache
                val cached = placeDetailsDao.getPlaceDetails(place.id)
                val isErrorCache = cached?.aiDescription?.startsWith("Gemini Error") == true || 
                                   cached?.aiDescription?.startsWith("Exception") == true ||
                                   cached?.aiDescription?.startsWith("Failed to parse") == true
                                   
                // Temporarily ignore cache if photoUrl is null or if it's an error cache
                if (cached != null && !isErrorCache && cached.photoUrls != null && cached.aiDescription != null) {
                    _selectedPlaceDetails.value = cached
                    _isFetchingPlaceDetails.value = false
                    return@launch
                }

                // 2. Query Foursquare Places API
                val fsqApiKey = BuildConfig.FOURSQUARE_API_KEY
                val queryUrl = "https://api.foursquare.com/v3/places/search?query=${java.net.URLEncoder.encode(place.name, "UTF-8")}&near=${java.net.URLEncoder.encode(destination, "UTF-8")}&limit=1&fields=fsq_id,name,location,photos"
                
                var address: String? = null
                var foursquareId: String? = null
                var photoUrls: String? = null
                var rating: Double? = null
                var fsqDataString = ""

                try {
                    val url = URL(queryUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", fsqApiKey)
                    connection.setRequestProperty("Accept", "application/json")
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(response)
                        val results = root.getJSONArray("results")
                        if (results.length() > 0) {
                            val placeObj = results.getJSONObject(0)
                            foursquareId = placeObj.optString("fsq_id", null)
                            val location = placeObj.optJSONObject("location")
                            var parsedAddress = location?.optString("formatted_address", null)
                            if (parsedAddress.isNullOrEmpty()) {
                                parsedAddress = location?.optString("address", null)
                            }
                            address = parsedAddress
                            
                            val photosArray = placeObj.optJSONArray("photos")
                            if (photosArray != null) {
                                val extractedUrls = mutableListOf<String>()
                                for (i in 0 until minOf(3, photosArray.length())) {
                                    val photoObj = photosArray.getJSONObject(i)
                                    val prefix = photoObj.optString("prefix")
                                    val suffix = photoObj.optString("suffix")
                                    if (prefix.isNotEmpty() && suffix.isNotEmpty()) {
                                        extractedUrls.add("${prefix}original${suffix}")
                                    }
                                }
                                if (extractedUrls.isNotEmpty()) {
                                    photoUrls = extractedUrls.joinToString(",")
                                }
                            }
                            
                            fsqDataString = response // pass raw Foursquare JSON to Gemini
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Fallback to Unsplash if no photo was found on Foursquare
                var finalPhotoUrls = photoUrls ?: ""
                if (finalPhotoUrls.isEmpty()) {
                    val unsplashUrls = fetchUnsplashPhotos("${place.name} $destination", 3)
                    finalPhotoUrls = unsplashUrls.joinToString(",")
                }

                // 3. Query Gemini for AI tips and description
                var aiDescription: String? = null
                var aiTip: String? = null
                var aiPrice: String? = null
                var aiHours: String? = null
                
                try {
                    val prompt = """
                        You are a local travel guide. 
                        I am visiting: ${place.name} in $destination.
                        Here is some raw Foursquare JSON data about the place (if available): $fsqDataString
                        
                        Write a short, engaging 2-sentence description for a traveler visiting this place.
                        Then, provide one helpful 'Local Tip'.
                        Extract or estimate the generic 'price'. You MUST format it intuitively across two lines like this: '[Local Currency Amount] VND\n(~$[USD Amount])' (e.g. '40,000 - 65,000 VND\n(~$2 - $3)' or just 'Free Entry'). 
                        Also provide the typical 'hours' including active days. Format 'hours' with a newline (\n) separating the days and times (e.g. 'Mon-Sun\n6:00 AM - 10:00 PM', 'Daily\nOpen 24/7').
                        Finally, provide the full, real-world street address for this place (e.g. '97 Vo Van Tan, District 3, Ho Chi Minh City'). Use the provided JSON or your own knowledge.
                        
                        Return ONLY a JSON object with this exact format, do not include any other conversational text or markdown blocks:
                        {"description": "2-sentence engaging description...", "tip": "Local tip...", "price": "40,000 VND\n(~$2)", "hours": "Mon-Sun\n6:00 AM - 10:00 PM", "address": "Full street address"}
                    """.trimIndent()

                    val aiResponseText = generateWithRest(prompt)
                    
                    try {
                        val startIndex = aiResponseText.indexOf("{")
                        val endIndex = aiResponseText.lastIndexOf("}")
                        if (startIndex != -1 && endIndex != -1) {
                            val cleanJsonStr = aiResponseText.substring(startIndex, endIndex + 1)
                            val parsedAi = JSONObject(cleanJsonStr)
                            aiDescription = parsedAi.optString("description", null)
                            aiTip = parsedAi.optString("tip", null)
                            aiPrice = parsedAi.optString("price", null)
                            aiHours = parsedAi.optString("hours", null)
                            val geminiAddress = parsedAi.optString("address", null)
                            if (!geminiAddress.isNullOrEmpty() && (address.isNullOrEmpty() || address == "null")) {
                                address = geminiAddress
                            }
                        } else {
                            if (cached != null && !isErrorCache) {
                                aiDescription = cached.aiDescription
                                aiTip = cached.aiTip
                                aiPrice = cached.price
                                aiHours = cached.formattedHours
                            } else {
                                aiDescription = "Gemini returned non-JSON format."
                            }
                        }
                    } catch (e: Exception) {
                        if (cached != null && !isErrorCache) {
                            aiDescription = cached.aiDescription
                            aiTip = cached.aiTip
                            aiPrice = cached.price
                            aiHours = cached.formattedHours
                        } else {
                            aiDescription = "Failed to parse Gemini JSON: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    if (cached != null && !isErrorCache) {
                        aiDescription = cached.aiDescription
                        aiTip = cached.aiTip
                        aiPrice = cached.price
                        aiHours = cached.formattedHours
                    } else {
                        aiDescription = "Exception querying Gemini: ${e.message}"
                    }
                    e.printStackTrace()
                }

                // 4. Save and Update
                val newDetails = com.example.pocketplanner.data.local.entity.PlaceDetailsEntity(
                    placeId = place.id,
                    foursquareId = foursquareId,
                    address = address,
                    aiDescription = aiDescription,
                    aiTip = aiTip,
                    photoUrls = finalPhotoUrls,
                    price = aiPrice,
                    formattedHours = aiHours
                )
                
                // Only cache if we actually successfully fetched something!
                if (address != null || aiDescription != null) {
                    placeDetailsDao.insertPlaceDetails(newDetails)
                }
                
                _selectedPlaceDetails.value = newDetails

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isFetchingPlaceDetails.value = false
            }
        }
    }

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
                _coverPhotoUrl.value = fetchUnsplashPhotos(destination, 1).firstOrNull()
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

    fun getPlacesForDay(tripId: String, dayNumber: Int): kotlinx.coroutines.flow.Flow<List<PlaceEntity>> {
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        return placeDao.getPlacesForDay(tripId, dayNumber).map { list ->
            list.sortedBy { place ->
                try { format.parse(place.startTime)?.time ?: 0L } catch (e: Exception) { 0L }
            }
        }
    }
    
    fun getAllPlaces(tripId: String): kotlinx.coroutines.flow.Flow<List<PlaceEntity>> {
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        return placeDao.getAllPlacesForTrip(tripId).map { list ->
            list.sortedBy { place ->
                try { format.parse(place.startTime)?.time ?: 0L } catch (e: Exception) { 0L }
            }
        }
    }

    fun getTrip(tripId: String) = tripRepository.getTrip(tripId)

    private val _geminiSuggestion = MutableStateFlow<String?>(null)
    val geminiSuggestion: StateFlow<String?> = _geminiSuggestion.asStateFlow()

    fun clearGeminiSuggestion() {
        _geminiSuggestion.value = null
    }

    fun searchWithGemini(query: String, destinationCity: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearchingPlaces.value = true
            try {
                val prompt = if (query.isBlank()) {
                    "You are a local tour guide in $destinationCity. Suggest EXACTLY 1 specific, highly-rated place (landmark, cafe, hidden gem) that a tourist MUST visit in $destinationCity. Return a JSON object with keys: 'name', 'address', 'lat' (approximate latitude float), 'lng' (approximate longitude float), and 'category' (e.g. 'Cafe', 'Museum'). Return ONLY valid JSON, no markdown formatting."
                } else {
                    "The user is searching for '$query' in $destinationCity. Provide the real details for this place so it can be mapped. Return a JSON object with keys: 'name' (clean official name), 'address' (estimated street address), 'lat' (approximate latitude float), 'lng' (approximate longitude float), and 'category'. Return ONLY valid JSON, no markdown formatting."
                }
                
                val jsonPayload = JSONObject()
                val contentsArray = JSONArray()
                val partsArray = JSONArray()
                val textPart = JSONObject().put("text", prompt)
                partsArray.put(textPart)
                
                val contentObj = JSONObject().put("role", "user").put("parts", partsArray)
                contentsArray.put(contentObj)
                jsonPayload.put("contents", contentsArray)

                val url = URL(endpointUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(response)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var text = parts.getJSONObject(0).optString("text", "").trim()
                            text = text.replace("```json", "").replace("```", "").trim()
                            
                            if (text.isNotBlank()) {
                                try {
                                    val obj = JSONObject(text)
                                    val result = SearchResult(
                                        fsqId = "gemini_${System.currentTimeMillis()}",
                                        name = obj.optString("name", query.ifBlank { "Unknown AI Place" }),
                                        address = obj.optString("address", destinationCity),
                                        lat = obj.optDouble("lat", 0.0),
                                        lng = obj.optDouble("lng", 0.0),
                                        category = obj.optString("category", "Gemini Pick") + " ✨"
                                    )
                                    _searchResults.value = listOf(result)
                                } catch (e: Exception) {
                                    _searchResults.value = listOf(SearchResult("err", "Gemini Error", "Could not parse AI response", 0.0, 0.0, "Error"))
                                }
                            }
                        }
                    }
                } else {
                    _searchResults.value = listOf(SearchResult("err", "Gemini API Error", conn.responseCode.toString(), 0.0, 0.0, "Error"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = listOf(SearchResult("err", "Exception", e.message ?: "Unknown", 0.0, 0.0, "Error"))
            } finally {
                _isSearchingPlaces.value = false
            }
        }
    }

    fun searchPlaces(query: String, lat: Double?, lng: Double?, destinationCity: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _isSearchingPlaces.value = true
            try {
                val mapboxToken = BuildConfig.MAPBOX_ACCESS_TOKEN
                if (mapboxToken.isNotBlank()) {
                    var proximity = ""
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        proximity = "&proximity=$lng,$lat"
                    }
                    // Limit search to Vietnam (country=vn)
                    val queryUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places/${java.net.URLEncoder.encode(query, "UTF-8")}.json?access_token=$mapboxToken$proximity&country=vn&limit=10"
                    
                    val url = URL(queryUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = org.json.JSONObject(response)
                        val features = root.optJSONArray("features") ?: org.json.JSONArray()
                        
                        val parsedResults = mutableListOf<SearchResult>()
                        for (i in 0 until features.length()) {
                            val feature = features.getJSONObject(i)
                            val fsqId = feature.optString("id") // Mapbox ID
                            val name = feature.optString("text")
                            val address = feature.optString("place_name")
                            
                            val center = feature.optJSONArray("center")
                            var parsedLng = 0.0
                            var parsedLat = 0.0
                            if (center != null && center.length() >= 2) {
                                parsedLng = center.optDouble(0, 0.0)
                                parsedLat = center.optDouble(1, 0.0)
                            }
                            
                            val properties = feature.optJSONObject("properties")
                            val category = properties?.optString("category", "Custom") ?: "Custom"
                            
                            parsedResults.add(SearchResult(fsqId, name, address, parsedLat, parsedLng, category))
                        }
                        _searchResults.value = parsedResults
                    } else {
                        val errorStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error stream"
                        _searchResults.value = listOf(
                            SearchResult("err", "Mapbox Error ${conn.responseCode}", errorStr, 0.0, 0.0, "Error")
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = listOf(
                    SearchResult("err", "Exception", e.message ?: "Unknown error", 0.0, 0.0, "Error")
                )
            } finally {
                _isSearchingPlaces.value = false
            }
        }
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun addSelectedPlace(tripId: String, dayNumber: Int, result: SearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get current places to calculate sequential time
                val currentPlaces = tripRepository.getPlacesForDay(tripId, dayNumber).firstOrNull() ?: emptyList()
                val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                val sorted = currentPlaces.sortedBy { try { format.parse(it.startTime)?.time ?: 0L } catch(e:Exception){0L} }
                val lastPlace = sorted.lastOrNull()
                
                var newStartTime = "05:00 PM"
                var newEndTime = "06:00 PM"
                
                if (lastPlace != null && lastPlace.endTime.isNotBlank()) {
                    newStartTime = lastPlace.endTime
                    try {
                        val d = format.parse(lastPlace.endTime)
                        if (d != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = d
                            cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                            newEndTime = format.format(cal.time)
                        }
                    } catch (e: Exception) {}
                }

                // 3. Insert into Database
                val newPlace = PlaceEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    dayNumber = dayNumber,
                    name = result.name,
                    lat = result.lat,
                    lng = result.lng,
                    category = result.category,
                    startTime = newStartTime,
                    endTime = newEndTime,
                    photoUrl = null
                )
                
                placeDao.insertPlaces(listOf(newPlace))
                fetchPhotoForPlace(newPlace, "") // Kick off background photo fetch
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlace(place: PlaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            placeDao.deletePlace(place)
        }
    }

    fun updatePlace(place: PlaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            placeDao.updatePlace(place)
        }
    }

    private val _isOptimizingRoute = MutableStateFlow(false)
    val isOptimizingRoute: StateFlow<Boolean> = _isOptimizingRoute.asStateFlow()

    fun optimizeRouteForDay(tripId: String, dayNumber: Int, destinationCity: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOptimizingRoute.value = true
            try {
                val currentPlaces = tripRepository.getPlacesForDay(tripId, dayNumber).firstOrNull() ?: emptyList()
                if (currentPlaces.size < 2) return@launch // Nothing to optimize

                val placesJsonStr = org.json.JSONArray(currentPlaces.map { 
                    org.json.JSONObject().put("id", it.id).put("name", it.name).put("category", it.category)
                }).toString()

                val prompt = "You are a master trip planner in $destinationCity. The user has these places scheduled for today: $placesJsonStr. Re-order these places geographically to minimize travel time, and assign realistic start and end times between 08:30 AM and 09:00 PM based on what time of day is best for each category (e.g. cafes in morning, bars at night) and typical opening hours for these specific locations. Return a JSON array of objects, where each object has exactly: 'id' (the exact id from input), 'startTime' (e.g. '08:30 AM'), and 'endTime' (e.g. '10:00 AM'). Ensure the times are strictly sequential and do not overlap, with at least 15-30 minutes of travel time between places. Return ONLY the raw valid JSON array."
                
                val jsonPayload = JSONObject()
                val contentsArray = JSONArray()
                val partsArray = JSONArray()
                val textPart = JSONObject().put("text", prompt)
                partsArray.put(textPart)
                
                val contentObj = JSONObject().put("role", "user").put("parts", partsArray)
                contentsArray.put(contentObj)
                jsonPayload.put("contents", contentsArray)

                val url = URL(endpointUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(response)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var text = parts.getJSONObject(0).optString("text", "").trim()
                            text = text.replace("```json", "").replace("```", "").trim()
                            
                            val jsonArray = JSONArray(text)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val id = obj.optString("id")
                                val startTime = obj.optString("startTime")
                                val endTime = obj.optString("endTime")
                                
                                val place = currentPlaces.find { it.id == id }
                                if (place != null && startTime.isNotBlank() && endTime.isNotBlank()) {
                                    placeDao.updatePlace(place.copy(startTime = startTime, endTime = endTime))
                                }
                            }
                        }
                    }
                }
            } catch(e: Exception) {
               e.printStackTrace()
            } finally {
                _isOptimizingRoute.value = false
            }
        }
    }

    fun toggleVisited(place: PlaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            placeDao.updatePlace(place.copy(isVisited = !place.isVisited))
        }
    }

    fun movePlaceDay(place: PlaceEntity, newDayNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            placeDao.updatePlace(place.copy(dayNumber = newDayNumber))
        }
    }

    fun swapPlaceOrder(place1: PlaceEntity, place2: PlaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val p1StartTime = place1.startTime
            val p1EndTime = place1.endTime
            val p2StartTime = place2.startTime
            val p2EndTime = place2.endTime
            
            placeDao.updatePlace(place1.copy(startTime = p2StartTime, endTime = p2EndTime))
            placeDao.updatePlace(place2.copy(startTime = p1StartTime, endTime = p1EndTime))
        }
    }

    fun swapDays(tripId: String, day1: Int, day2: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val placesDay1 = placeDao.getPlacesForDaySync(tripId, day1)
            val placesDay2 = placeDao.getPlacesForDaySync(tripId, day2)
            
            placesDay1.forEach { placeDao.updatePlace(it.copy(dayNumber = day2)) }
            placesDay2.forEach { placeDao.updatePlace(it.copy(dayNumber = day1)) }
        }
    }

    suspend fun geocodeCity(city: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val fsqApiKey = BuildConfig.FOURSQUARE_API_KEY
                if (fsqApiKey.isNotBlank()) {
                    val queryUrl = "https://api.foursquare.com/v3/places/search?near=${java.net.URLEncoder.encode(city, "UTF-8")}&limit=1&fields=geocodes"
                    val url = URL(queryUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", fsqApiKey)
                    conn.setRequestProperty("Accept", "application/json")
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = org.json.JSONObject(response)
                        val results = root.getJSONArray("results")
                        if (results.length() > 0) {
                            val geo = results.getJSONObject(0).optJSONObject("geocodes")?.optJSONObject("main")
                            if (geo != null) {
                                return@withContext Pair(geo.optDouble("latitude", 0.0), geo.optDouble("longitude", 0.0))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

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
        isOpenEnded: Boolean = false,
        onSuccess: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                var prompt = """
                    You are a professional travel planner. Create a $days day itinerary for a trip to "$destination".
                    The user's trip is named "$name".
                    
                    CRITICAL REQUIREMENT:
                    1. You must group locations geographically by day to minimize travel time! Do NOT suggest locations that are hours apart on the same day. Each day's itinerary should focus on a specific neighborhood, region, or district where places are within walking distance or a short transit ride from one another.
                    2. You MUST generate exactly 7 places to visit per day.
                    3. You MUST include local cuisine and highly-rated restaurants in the itinerary (e.g. for breakfast, lunch, and dinner).
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
                              "notes": "Book tickets in advance.",
                              "startTime": "09:00 AM",
                              "endTime": "11:30 AM"
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
                val photoUrl = customPhotoUrl ?: _coverPhotoUrl.value ?: fetchUnsplashPhotos(destination, 1).firstOrNull()

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
                    trackingMode = trackingMode,
                    isOpenEnded = isOpenEnded
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

                    val dailyPlaces = mutableListOf<PlaceEntity>()

                    for (j in 0 until placesArray.length()) {
                        val placeObj = placesArray.getJSONObject(j)
                        dailyPlaces.add(
                            PlaceEntity(
                                id = UUID.randomUUID().toString(),
                                tripId = tripId,
                                dayNumber = dayNum,
                                name = placeObj.getString("name"),
                                lat = placeObj.getDouble("lat"),
                                lng = placeObj.getDouble("lng"),
                                category = placeObj.getString("category"),
                                estimatedCost = placeObj.optDouble("estimatedCost", 0.0),
                                notes = placeObj.optString("notes", ""),
                                startTime = placeObj.optString("startTime", ""),
                                endTime = placeObj.optString("endTime", "")
                            )
                        )
                    }
                    
                    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                    dailyPlaces.sortBy { 
                        try { format.parse(it.startTime)?.time ?: 0L } catch (e: Exception) { 0L }
                    }
                    placesList.addAll(dailyPlaces)
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
        isOpenEnded: Boolean = false,
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
                
                val oldTotalDays = if (oldTrip.isOpenEnded) 3 else ((oldTrip.endDate - oldTrip.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                val newTotalDays = if (isOpenEnded) 3 else ((newEnd - newStart) / 86400000L).toInt().coerceAtLeast(0) + 1
                val budgetChanged = kotlin.math.abs(budgetInVnd - oldTrip.budget) > 1.0
                
                if (newTotalDays > oldTotalDays || budgetChanged) {
                    _isGenerating.value = true
                }
                
                if (newTotalDays < oldTotalDays) {
                    tripRepository.deletePlacesForDaysGreaterThan(tripId, newTotalDays)
                } else if (newTotalDays > oldTotalDays && !isOpenEnded) { // If open-ended, don't auto-extend itinerary on edit
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
                        
                        CRITICAL REQUIREMENT:
                        1. You must group locations geographically by day to minimize travel time! Do NOT suggest locations that are hours apart on the same day. Each day's itinerary should focus on a specific neighborhood, region, or district where places are within walking distance or a short transit ride from one another.
                        2. You MUST generate exactly 7 places to visit per day.
                        3. You MUST include local cuisine and highly-rated restaurants in the itinerary (e.g. for breakfast, lunch, and dinner).
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
                                  "notes": "Book tickets in advance.",
                                  "startTime": "09:00 AM",
                                  "endTime": "11:30 AM"
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
                        
                        val dailyPlaces = mutableListOf<PlaceEntity>()

                        for (j in 0 until placesArray.length()) {
                            val placeObj = placesArray.getJSONObject(j)
                            dailyPlaces.add(
                                PlaceEntity(
                                    id = UUID.randomUUID().toString(),
                                    tripId = tripId,
                                    dayNumber = dayNum,
                                    name = placeObj.getString("name"),
                                    lat = placeObj.getDouble("lat"),
                                    lng = placeObj.getDouble("lng"),
                                    category = placeObj.getString("category"),
                                    estimatedCost = placeObj.optDouble("estimatedCost", 0.0),
                                    notes = placeObj.optString("notes", ""),
                                    startTime = placeObj.optString("startTime", ""),
                                    endTime = placeObj.optString("endTime", "")
                                )
                            )
                        }
                        
                        // Sort daily places chronologically to fix AI out-of-order output
                        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                        dailyPlaces.sortBy { 
                            try { format.parse(it.startTime)?.time ?: 0L } catch (e: Exception) { 0L }
                        }
                        placesList.addAll(dailyPlaces)
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
                    isOpenEnded = isOpenEnded,
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

    private suspend fun fetchUnsplashPhotos(destination: String, count: Int = 1): List<String> = withContext(Dispatchers.IO) {
        try {
            val unsplashKey = BuildConfig.UNSPLASH_API_KEY
            if (unsplashKey.isBlank()) return@withContext emptyList()
            
            val query = java.net.URLEncoder.encode(destination, "UTF-8")
            val url = URL("https://api.unsplash.com/search/photos?query=${query}&per_page=$count&client_id=$unsplashKey")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseString)
                val resultsArray = jsonObject.optJSONArray("results")
                if (resultsArray != null && resultsArray.length() > 0) {
                    val urls = mutableListOf<String>()
                    for (i in 0 until resultsArray.length()) {
                        val result = resultsArray.getJSONObject(i)
                        urls.add(result.getJSONObject("urls").getString("regular"))
                    }
                    return@withContext urls
                }
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
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
                
                val descriptionRes = when (code) {
                    0 -> R.string.weather_clear
                    1, 2, 3 -> R.string.weather_cloudy
                    45, 48 -> R.string.weather_foggy
                    51, 53, 55 -> R.string.weather_drizzling
                    61, 63, 65 -> R.string.weather_raining
                    71, 73, 75 -> R.string.weather_snowing
                    95, 96, 99 -> R.string.weather_thunderstorms
                    else -> R.string.weather_variable
                }

                _weatherState.value = WeatherState.Success(city, temp, descriptionRes)
            } catch (e: Exception) {
                e.printStackTrace()
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun addDetailedPlaceToDay(
        tripId: String,
        dayNumber: Int,
        name: String,
        category: String = "Sightseeing",
        lat: Double = 0.0, // Defaulting to 0.0 for mock data, can be updated later
        lng: Double = 0.0,
        photoUrl: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get current places to calculate sequential time (Reusing your logic!)
                val currentPlaces = tripRepository.getPlacesForDay(tripId, dayNumber).firstOrNull() ?: emptyList()
                val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                val sorted = currentPlaces.sortedBy { try { format.parse(it.startTime)?.time ?: 0L } catch(e:Exception){0L} }
                val lastPlace = sorted.lastOrNull()

                var newStartTime = "09:00 AM" // Default morning start time
                var newEndTime = "10:00 AM"

                if (lastPlace != null && lastPlace.endTime.isNotBlank()) {
                    newStartTime = lastPlace.endTime
                    try {
                        val d = format.parse(lastPlace.endTime)
                        if (d != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = d
                            cal.add(java.util.Calendar.HOUR_OF_DAY, 1) // Add 1 hour duration
                            newEndTime = format.format(cal.time)
                        }
                    } catch (e: Exception) {}
                }

                // 2. Insert into Database
                val newPlace = PlaceEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    dayNumber = dayNumber,
                    name = name,
                    lat = lat,
                    lng = lng,
                    category = category,
                    startTime = newStartTime,
                    endTime = newEndTime,
                    photoUrl = photoUrl
                )

                placeDao.insertPlaces(listOf(newPlace))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val city: String, val temperature: Double, val descriptionRes: Int) : WeatherState()
    data class Error(val message: String) : WeatherState()
}