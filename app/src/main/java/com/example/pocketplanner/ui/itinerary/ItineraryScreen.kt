package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onDayClick: (dayNumber: Int) -> Unit,
    onExpenseClick: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel()
) {
    // Observe the specific trip from the DB
    val trip by viewModel.getTrip(tripId).collectAsState(initial = null)
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.destination ?: "Trip Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onExpenseClick,
                icon = { Icon(Icons.Default.AttachMoney, "Expenses") },
                text = { Text("Expenses") }
            )
        }
    ) { innerPadding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    "Status: ${trip!!.status}", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${dateFormatter.format(Date(trip!!.startDate))} - ${dateFormatter.format(Date(trip!!.endDate))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Your Itinerary", 
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Calculate days difference
                val diffInMillies = Math.abs(trip!!.endDate - trip!!.startDate)
                val daysCount = (diffInMillies / 86400000L).toInt()
                
                // If the dates are the same, it's at least a 1 day trip
                val totalDays = if (daysCount <= 0) 1 else daysCount
                
                for (day in 1..totalDays) { 
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onDayClick(day) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Day $day", style = MaterialTheme.typography.titleMedium)
                            Text("View Plan >", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}