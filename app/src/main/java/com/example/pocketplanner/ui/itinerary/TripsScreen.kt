package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.data.local.entity.TripEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: ItineraryViewModel = hiltViewModel(),
    userId: String, // We pass the logged-in user's ID here
    onTripClick: (tripId: String) -> Unit = {}
) {
    // Tell the ViewModel to load trips for this user as soon as the screen opens
    LaunchedEffect(userId) {
        viewModel.loadTrips(userId)
    }

    // Observe the list of trips from the ViewModel
    val trips by viewModel.trips.collectAsState()

    // State for the Create Trip Popup
    var showCreateDialog by remember { mutableStateOf(false) }
    var destinationInput by remember { mutableStateOf("") }
    var daysInput by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Trips", color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Trip", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        if (trips.isEmpty()) {
            // Show a friendly empty state if they have no trips
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No trips planned yet!\nTap the + button to use AI.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Display the list of trips
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(trips) { trip ->
                    TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }
        }
    }

    // The Create Trip Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Plan a New Trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Where do you want to go?")
                    OutlinedTextField(
                        value = destinationInput,
                        onValueChange = { destinationInput = it },
                        placeholder = { Text("e.g. Paris, France") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("How many days?")
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysInput.toIntOrNull() ?: 5
                        if (destinationInput.isNotBlank()) {
                            viewModel.generateTripWithAI(userId, destinationInput, days)
                            showCreateDialog = false
                            destinationInput = "" // Reset for next time
                            daysInput = "5"
                        }
                    }
                ) {
                    Text("Generate with AI ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCard(trip: TripEntity, onClick: () -> Unit = {}) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = trip.destination,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${dateFormatter.format(Date(trip.startDate))} - ${dateFormatter.format(Date(trip.endDate))}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${trip.status}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}