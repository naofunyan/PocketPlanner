package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.pocketplanner.R
import kotlinx.coroutines.launch
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

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: ItineraryViewModel = hiltViewModel(),
    userId: String,
    onTripClick: (tripId: String) -> Unit = {},
    onEditTripClick: (tripId: String) -> Unit = {},
    onAddTripClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(userId) {
        viewModel.loadTrips(userId)
    }

    val trips by viewModel.trips.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    
    var showContextMenuForTrip by remember { mutableStateOf<TripEntity?>(null) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true || permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.fetchWeather(context, location.latitude, location.longitude)
                    } else {
                        viewModel.fetchWeather(context, 10.7626, 106.6602) // Default to N/A
                    }
                }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.fetchWeather(context, location.latitude, location.longitude)
                    } else {
                        viewModel.fetchWeather(context, 10.7626, 106.6602)
                    }
                }
            } catch (e: SecurityException) { }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

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
                activeTrip = trips.firstOrNull(),
                weatherState = weatherState,
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
                            Text(
                                text = "Kick things off by adding a future adventure",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    val now = System.currentTimeMillis()
                    val activeTrips = trips.filter { now in it.startDate..it.endDate || it.status == "ACTIVE" }.sortedBy { it.startDate }
                    val upcomingTrips = trips.filter { now < it.startDate && it.status != "ACTIVE" }.sortedBy { it.startDate }
                    val pastTrips = trips.filter { now > it.endDate && it.status != "ACTIVE" }.sortedByDescending { it.endDate }

                    if (activeTrips.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Trips",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(activeTrips) { trip ->
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                TripCard(trip = trip, onClick = { onTripClick(trip.id) }, onLongClick = { showContextMenuForTrip = trip })
                            }
                        }
                    }

                    if (upcomingTrips.isNotEmpty()) {
                        item {
                            Text(
                                text = "Upcoming Trips",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(upcomingTrips) { trip ->
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                TripCard(trip = trip, onClick = { onTripClick(trip.id) }, onLongClick = { showContextMenuForTrip = trip })
                            }
                        }
                    }

                    if (pastTrips.isNotEmpty()) {
                        item {
                            Text(
                                text = "Past Trips",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(pastTrips) { trip ->
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                TripCard(trip = trip, onClick = { onTripClick(trip.id) }, onLongClick = { showContextMenuForTrip = trip })
                            }
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

    val coroutineScope = rememberCoroutineScope()
    
    var tripToDelete by remember { mutableStateOf<com.example.pocketplanner.data.local.entity.TripEntity?>(null) }

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

    if (showContextMenuForTrip != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showContextMenuForTrip = null },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Settings
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Trip Settings") },
                    leadingContent = { Icon(painter = painterResource(id = R.drawable.navsettings), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    modifier = Modifier.clickable {
                        val tripId = showContextMenuForTrip?.id
                        showContextMenuForTrip = null
                        if (tripId != null) {
                            onEditTripClick(tripId)
                        }
                    }
                )
                // Share
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Share Trip") },
                    leadingContent = { Icon(painter = painterResource(id = R.drawable.share), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    modifier = Modifier.clickable {
                        val trip = showContextMenuForTrip
                        showContextMenuForTrip = null
                        if (trip != null) {
                            coroutineScope.launch {
                                // 1. Try to generate a postcard image
                                val imageUri = com.example.pocketplanner.util.ImageGenerator.generateTripPostcard(context, trip)
                                
                                // 2. Create the rich text with Deep Link
                                val deepLinkUrl = "pocketplanner://trip/${trip.id}"
                                val shareText = "✈️ Check out my trip to ${trip.destination}!\n\nImport it directly into PocketPlanner:\n$deepLinkUrl"
                                
                                // 3. Build and launch intent
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    if (imageUri != null) {
                                        putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
                                        type = "image/jpeg"
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    } else {
                                        type = "text/plain"
                                    }
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Trip")
                                context.startActivity(shareIntent)
                            }
                        }
                    }
                )
                // Delete
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Delete Trip", color = Color.Red) },
                    leadingContent = { Icon(painter = painterResource(id = R.drawable.delete), contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp)) },
                    modifier = Modifier.clickable {
                        val trip = showContextMenuForTrip
                        showContextMenuForTrip = null
                        if (trip != null) {
                            tripToDelete = trip
                        }
                    }
                )
            }
        }
    }

    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Delete Trip?") },
            text = { 
                Column {
                    Text("Are you sure you want to delete this trip?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All the steps for the trip, including pictures, locations, and text updates will be permanently deleted.", color = Color.DarkGray)
                } 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tripToDelete?.id?.let { viewModel.deleteTrip(it) }
                        tripToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HomeHeader(userName: String, activeTrip: TripEntity?, weatherState: WeatherState, modifier: Modifier = Modifier, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greetingText = when (currentHour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    var countdownText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(activeTrip) {
        if (activeTrip == null) {
            countdownText = "You have no upcoming trips planned yet."
        } else {
            while (true) {
                val now = System.currentTimeMillis()
                val start = activeTrip.startDate
                if (now >= start) {
                    countdownText = "Your trip to ${activeTrip.destination} is happening right now!"
                    break
                } else {
                    val diff = start - now
                    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff) % 24
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    
                    val parts = mutableListOf<String>()
                    if (days > 0) parts.add(String.format("%02d days", days))
                    if (hours > 0 || days > 0) parts.add(String.format("%02d hours", hours))
                    parts.add(String.format("%02d minutes", minutes))
                    parts.add(String.format("%02d seconds", seconds))
                    
                    countdownText = "Your trip to ${activeTrip.destination} will begin in ${parts.joinToString(" ")}."
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

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
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.pocketplanner.R.drawable.logo),
                    contentDescription = "PocketPlanner Logo",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PocketPlanner",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Greeting
            Text(
                text = "$greetingText, $userName!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Live Countdown
            Text(
                text = countdownText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Pill
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
                    
                    val weatherString = when (weatherState) {
                        is WeatherState.Loading -> "Fetching local weather..."
                        is WeatherState.Success -> {
                            val tempC = weatherState.temperature.toInt()
                            val tempF = (weatherState.temperature * 9 / 5 + 32).toInt()
                            "The weather in ${weatherState.city} is $tempC°C / $tempF°F, and will be ${weatherState.description}."
                        }
                        is WeatherState.Error -> "Weather data unavailable."
                    }
                    
                    Text(
                        text = weatherString,
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
                Icon(Icons.Filled.Add, contentDescription = "Add a trip", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add a trip", style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TripCard(trip: TripEntity, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())
    
    val start = Date(trip.startDate)
    val end = Date(trip.endDate)
    val now = System.currentTimeMillis()
    
    // Add 1 to totalDays so a trip from Aug 15 to Aug 15 is 1 day.
    val totalDays = ((trip.endDate - trip.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
    val daysSpent = if (now < trip.startDate) 0 else if (now > trip.endDate) totalDays else ((now - trip.startDate) / 86400000L).toInt() + 1
    
    val isTravelingNow = now in trip.startDate..trip.endDate

    // Generate a beautiful mock image based on the trip's ID so it stays consistent
    val mockImageUrl = "https://picsum.photos/seed/${trip.id}/800/400"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(200.dp) // slightly adjusted height
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            AsyncImage(
                model = trip.photoUrl ?: mockImageUrl,
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // "NOW TRAVELING" Badge (Top Left)
            if (isTravelingNow || trip.status == "ACTIVE") {
                Surface(
                    color = Color(0xFF1E3A4B).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#now_travelling",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Text content anchored to bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val titleText = trip.name.takeIf { it.isNotBlank() } ?: "Trip to ${trip.destination}"
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Stats Row
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Date / Year
                        Column {
                            Text(
                                text = "${dateFormatter.format(start)} - ${dateFormatter.format(end)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = yearFormatter.format(start),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Days
                        Column {
                            Text(
                                text = "$daysSpent/$totalDays",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "DAYS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}