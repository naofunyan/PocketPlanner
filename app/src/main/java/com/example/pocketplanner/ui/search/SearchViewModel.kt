package com.example.pocketplanner.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class MapboxPlace(
    val mainText: String,
    val secondaryText: String,
    val placeType: String,
    val countryCode: String? = null,
    val emojiFlag: String? = null
)

fun countryCodeToEmoji(countryCode: String?): String? {
    if (countryCode == null || countryCode.length != 2) return null
    val firstLetter = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MapboxPlace>>(emptyList())
    val searchResults: StateFlow<List<MapboxPlace>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                    } else {
                        fetchSuggestions(query)
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    private suspend fun fetchSuggestions(query: String) {
        _isLoading.value = true
        withContext(Dispatchers.IO) {
            try {
                val token = BuildConfig.MAPBOX_ACCESS_TOKEN
                if (token.isBlank()) {
                    _isLoading.value = false
                    return@withContext
                }

                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                // Focus strictly on places, regions, and countries (ignoring specific street addresses)
                val urlString = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json?access_token=$token&types=place,region,country&limit=5"
                
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseString)
                    val featuresArray = jsonObject.optJSONArray("features")
                    
                    val results = mutableListOf<MapboxPlace>()
                    if (featuresArray != null) {
                        for (i in 0 until featuresArray.length()) {
                            val feature = featuresArray.getJSONObject(i)
                            val text = feature.getString("text")
                            val placeName = feature.getString("place_name")
                            
                            var secondaryText = ""
                            if (placeName.startsWith("$text, ")) {
                                secondaryText = placeName.removePrefix("$text, ").trim()
                            } else if (placeName != text) {
                                secondaryText = placeName
                            }
                            
                            val placeTypeArray = feature.optJSONArray("place_type")
                            val rawPlaceType = if (placeTypeArray != null && placeTypeArray.length() > 0) {
                                placeTypeArray.getString(0)
                            } else {
                                "place"
                            }
                            
                            val placeType = when (rawPlaceType) {
                                "region" -> "States & Regions"
                                "country" -> "Countries"
                                "place" -> "Cities & Towns"
                                "poi" -> "Points of Interest"
                                else -> rawPlaceType.replaceFirstChar { it.uppercase() } + "s"
                            }
                            
                            var countryCode: String? = null
                            if (feature.has("properties")) {
                                val props = feature.getJSONObject("properties")
                                if (props.has("short_code")) {
                                    countryCode = props.getString("short_code").take(2)
                                }
                            }
                            if (countryCode == null && feature.has("context")) {
                                val contextArray = feature.getJSONArray("context")
                                for (j in 0 until contextArray.length()) {
                                    val ctx = contextArray.getJSONObject(j)
                                    val id = ctx.optString("id", "")
                                    if (id.startsWith("country")) {
                                        if (ctx.has("short_code")) {
                                            countryCode = ctx.getString("short_code").take(2)
                                        }
                                        break
                                    }
                                }
                            }
                            
                            val emojiFlag = countryCodeToEmoji(countryCode)
                            
                            results.add(MapboxPlace(
                                mainText = text, 
                                secondaryText = secondaryText, 
                                placeType = placeType,
                                countryCode = countryCode,
                                emojiFlag = emojiFlag
                            ))
                        }
                    }
                    _searchResults.value = results
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
