package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

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
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
        topBar = {
            TopAppBar(
                title = { Text(String.format(stringResource(R.string.day_plan_title), dayNumber), color = MaterialTheme.colorScheme.onBackground) }, // DYNAMIC TEXT
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.day_plan_back_cd),
                            tint = MaterialTheme.colorScheme.onBackground // DYNAMIC ICON
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // DYNAMIC BACKGROUND
                )
            )
        }
    ) { innerPadding ->
        if (places.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center // Centers the loading text nicely
            ) {
                Text(stringResource(R.string.day_plan_loading), color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(places) { place ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC CARD BACKGROUND
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                            )
                            Text(
                                text = place.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary // DYNAMIC TEXT
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = place.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) // DYNAMIC TEXT
                            )
                        }
                    }
                }
            }
        }
    }
}