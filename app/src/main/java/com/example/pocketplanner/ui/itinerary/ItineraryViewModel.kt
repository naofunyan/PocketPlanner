package com.example.pocketplanner.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.TripRepository
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ItineraryViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    // Initialize Vertex AI for Firebase (No API key needed!)
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash")

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
                // 1. Ask Gemini to generate an itinerary
                val prompt = "Plan a $days day trip to $destination. Give me a 2 sentence summary of what the vibe will be."
                val response = generativeModel.generateContent(prompt)

                // 2. Create the Trip in our Database
                val newTrip = TripEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    destination = destination,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (days * 86400000L), // Add days in milliseconds
                    budget = 5000000.0, // Example budget
                    status = "UPCOMING"
                )

                tripRepository.createTrip(newTrip)

                // (In Part 2, we will also parse Gemini's response and save individual Places)
                println("Gemini Says: ${response.text}")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}