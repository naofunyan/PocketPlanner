package com.example.pocketplanner.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.ui.itinerary.ItineraryViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelStatsScreen(
    viewModel: ItineraryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadTrips(userId)
        }
    }

    val trips by viewModel.trips.collectAsState()

    // Calculate stats
    val totalTrips = trips.size
    
    var totalDays = 0L
    val uniqueDestinations = mutableSetOf<String>()
    var activeOrUpcomingCount = 0
    var pastTripsCount = 0
    
    val now = System.currentTimeMillis()

    trips.forEach { trip ->
        // Unique Destinations (using a simple heuristic for now, just the raw string)
        uniqueDestinations.add(trip.destination.trim().lowercase())
        
        // Categorize
        if (trip.status == "PAST" || (!trip.isOpenEnded && now > trip.endDate && trip.status != "ACTIVE")) {
            pastTripsCount++
        } else {
            activeOrUpcomingCount++
        }
        
        // Calculate days
        if (now < trip.startDate) {
            // Upcoming trip, 0 days experienced yet
        } else if (trip.isOpenEnded) {
            // Open ended and already started
            totalDays += ((now - trip.startDate) / 86400000L) + 1
        } else if (now in trip.startDate..trip.endDate) {
            // Currently active trip
            totalDays += ((now - trip.startDate) / 86400000L) + 1
        } else {
            // Past trip
            totalDays += ((trip.endDate - trip.startDate) / 86400000L).coerceAtLeast(0) + 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.stats_back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.stats_lifetime_journey),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.stats_lifetime_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                StatCard(
                    icon = Icons.Filled.FlightTakeoff,
                    title = stringResource(R.string.stats_total_trips),
                    value = totalTrips.toString(),
                    subtitle = String.format(stringResource(R.string.stats_trips_subtitle), pastTripsCount, activeOrUpcomingCount)
                )
            }
            
            item {
                StatCard(
                    icon = Icons.Filled.AccessTime,
                    title = stringResource(R.string.stats_days_on_road),
                    value = totalDays.toString(),
                    subtitle = stringResource(R.string.stats_days_subtitle)
                )
            }
            
            item {
                StatCard(
                    icon = Icons.Filled.Public,
                    title = stringResource(R.string.stats_unique_destinations),
                    value = uniqueDestinations.size.toString(),
                    subtitle = stringResource(R.string.stats_unique_subtitle)
                )
            }
            
            // We could add a list of recently visited destinations here if desired
            if (uniqueDestinations.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.stats_places_been),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Show up to 10 recent destinations
                items(uniqueDestinations.take(10).size) { index ->
                    val dest = uniqueDestinations.toList()[index]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = dest.split(",").joinToString { it.trim().replaceFirstChar { char -> char.uppercase() } },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun StatCard(icon: ImageVector, title: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
