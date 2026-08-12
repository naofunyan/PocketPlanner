package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pocketplanner.data.local.entity.TripEntity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: ItineraryViewModel = hiltViewModel(),
    userId: String,
    onTripClick: (tripId: String) -> Unit = {},
    onAddTripClick: () -> Unit = {}
) {
    LaunchedEffect(userId) {
        viewModel.loadTrips(userId)
    }

    val trips by viewModel.trips.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    
    // User info for the header
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"

    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val headerHeightDp = with(LocalDensity.current) { 
        if (headerHeightPx > 0) headerHeightPx.toDp() else 300.dp // Fallback height
    }

    // Parallax translation: Header moves up at half the speed of the list
    val headerTranslationY = if (listState.firstVisibleItemIndex == 0) {
        -listState.firstVisibleItemScrollOffset / 2f
    } else {
        -headerHeightPx / 2f
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. Bottom Layer: Parallax Header
            HomeHeader(
                userName = userName,
                topPadding = innerPadding.calculateTopPadding(),
                modifier = Modifier
                    .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
                    .graphicsLayer { translationY = headerTranslationY }
            )

            // 2. Top Layer: Scrolling Content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentPadding = PaddingValues(bottom = 140.dp) // Extra padding to scroll past the 120dp floating nav
            ) {
                // Transparent spacer to reveal the header underneath
                item {
                    Spacer(modifier = Modifier.height(maxOf(0.dp, headerHeightDp - 24.dp)))
                }

                // The white content area that slides UP over the header
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        ActionRow(onAddTripClick = onAddTripClick)
                    }
                }

                // Trips List
                if (trips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(32.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No trips planned yet!\nTap '+ Add trip' to use AI.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    items(trips) { trip ->
                        // Wrap in white background so the list creates a solid scrolling sheet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                        }
                    }
                }
                
                // Bottom filler to ensure we don't see the header at the very bottom if list is short
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }



    if (isGenerating) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismiss while generating */ },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is planning your trip...", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This usually takes a few seconds.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {} // No buttons, user must wait
        )
    }
}

@Composable
fun HomeHeader(userName: String, modifier: Modifier = Modifier, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = topPadding + 48.dp, bottom = 48.dp) // Extra bottom padding for the overlap
        ) {
            // App Logo / Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for Logo
                Box(
                    modifier = Modifier.size(32.dp).background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌍", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "PocketPlan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Greeting
            Text(
                text = "Good morning, $userName!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Mock Countdown
            Text(
                text = "Your trip to Tokyo will begin in 03 hours 10 minutes 10 seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mock Weather Pill
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "Weather",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "The weather in Tokyo is 34 degrees C, and will be cloudy at night.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ActionRow(onAddTripClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onAddTripClick,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp), // Override default padding for perfect centering
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Add, contentDescription = "Add trip", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add trip", style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
            }
        }

        OutlinedButton(
            onClick = { /* TODO: Stats */ },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp), // Override default padding for perfect centering
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.BarChart, contentDescription = "Travel stats", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Travel stats", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCard(trip: TripEntity, onClick: () -> Unit = {}) {
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    // Generate a beautiful mock image based on the trip's ID so it stays consistent
    val mockImageUrl = "https://picsum.photos/seed/${trip.id}/800/400"

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            AsyncImage(
                model = mockImageUrl,
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay so text is readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            // Text content anchored to bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dateFormatter.format(Date(trip.startDate))} - ${dateFormatter.format(Date(trip.endDate))} • Trip",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}