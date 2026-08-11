package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanScreen(
    tripId: String,
    dayNumber: Int,
    onNavigateBack: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel()
) {
    // Observe the places for this specific day from Room
    val places by viewModel.getPlacesForDay(tripId, dayNumber).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day $dayNumber") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { innerPadding ->
        if (places.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Text("Loading places from AI...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(places) { place ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                            Text(place.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(place.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}