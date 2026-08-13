package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.NotificationsActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onDayClick: (dayNumber: Int) -> Unit, // Might not need this anymore if handled internally, keeping for signature compatibility
    onExpenseClick: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel()
) {
    val trip by viewModel.getTrip(tripId).collectAsState(initial = null)

    var selectedDay by remember { mutableIntStateOf(1) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.enableProximityAlerts(tripId)
        }
    }
    
    // Fetch places for the currently selected day
    val places by viewModel.getPlacesForDay(tripId, selectedDay).collectAsState(initial = emptyList())

    val dateFormatter = SimpleDateFormat("EEE M/d", Locale.getDefault())

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 70.dp) // Lift above the custom bottom bar
            ) {
                // AI Sparkle FAB
                FloatingActionButton(
                    onClick = { /* TODO: AI Chat */ },
                    containerColor = Color(0xFFE65100), // Orange
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Magic")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add FAB
                FloatingActionButton(
                    onClick = { /* TODO: Add place */ },
                    containerColor = Color(0xFF1E1E1E), // Near Black
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Place")
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(
                    onClick = { 
                        val perms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    },
                    containerColor = Color(0xFF005b9f), // Blue
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = "Enable Geofence Alerts")
                }
            }
        }
    ) { innerPadding ->
        if (trip == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Cover Image (Top half)
                Image(
                    painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1555921015-c2620a56f6c1?q=80&w=800&auto=format&fit=crop"),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                )
                
                // Top controls overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(40.dp).clickable(onClick = onNavigateBack)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp), tint = Color.DarkGray)
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(40.dp).clickable { /* Menu */ }
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.padding(8.dp), tint = Color.DarkGray)
                    }
                }
                
                // Location Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF005b9f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip!!.destination,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }
                
                // Main Content Sheet (Overlapping the image)
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 350.dp) // Push down to let image show
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.LightGray)
                            )
                        }
                        
                        // Calculate Total Days
                        val diffInMillies = Math.abs(trip!!.endDate - trip!!.startDate)
                        val daysCount = (diffInMillies / 86400000L).toInt()
                        val totalDays = if (daysCount <= 0) 1 else daysCount
                        
                        // Date Tabs Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEEEEEE),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar", modifier = Modifier.padding(8.dp), tint = Color.DarkGray)
                                }
                            }
                            
                            items(totalDays) { index ->
                                val dayNum = index + 1
                                val isSelected = dayNum == selectedDay
                                
                                // Calculate Date
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = trip!!.startDate
                                calendar.add(Calendar.DAY_OF_YEAR, index)
                                val dateString = dateFormatter.format(calendar.time)
                                
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF1E1E1E) else Color(0xFFFAFAFA),
                                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)) else null,
                                    modifier = Modifier.clickable { selectedDay = dayNum }
                                ) {
                                    Text(
                                        text = dateString,
                                        color = if (isSelected) Color.White else Color.DarkGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        
                        // Day Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentCal = Calendar.getInstance()
                            currentCal.timeInMillis = trip!!.startDate
                            currentCal.add(Calendar.DAY_OF_YEAR, selectedDay - 1)
                            val currentDateStr = dateFormatter.format(currentCal.time)
                                
                            Column {
                                Text(currentDateStr, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                            
                            Text("Optimize route", color = Color(0xFF0091EA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        
                        // Timeline Places List
                        LazyColumn(
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp), // extra bottom padding for navbar
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (places.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No places added for this day yet.", color = Color.Gray)
                                    }
                                }
                            } else {
                                itemsIndexed(places) { index, place ->
                                    val isLastItem = index == places.lastIndex
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min) // Important to draw vertical line full height
                                    ) {
                                        // Left Timeline Column
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(40.dp)
                                        ) {
                                            // Circular Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF81D4FA), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF01579B),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            
                                            if (!isLastItem) {
                                                // Vertical connecting line
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .weight(1f)
                                                        .background(Color(0xFFBDBDBD))
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Right Card Column
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color.White,
                                                shadowElevation = 2.dp,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = place.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp,
                                                            color = Color.DarkGray,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFF5F5F5)
                                                        ) {
                                                            Text(
                                                                text = place.category,
                                                                fontSize = 12.sp,
                                                                color = Color.Gray,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    
                                                    // Thumbnail
                                                    Image(
                                                        painter = rememberAsyncImagePainter("https://picsum.photos/seed/${place.name.hashCode()}/200"),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFE0E0E0)) // Fallback gray box if image fails to load
                                                    )
                                                }
                                            }
                                            
                                            if (!isLastItem) {
                                                // Transit Row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 16.dp)
                                                ) {
                                                    Icon(Icons.Filled.DirectionsWalk, contentDescription = "Walk", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("15 mins • 0.8 mi ", fontSize = 12.sp, color = Color.Gray)
                                                    Text("Directions >", fontSize = 12.sp, color = Color(0xFF0091EA), fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Custom Pill Bottom Navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavPill("Plan", true) { }
                            BottomNavPill("Expense", false) { onExpenseClick() }
                            BottomNavPill("Track", false) { onTrackClick() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF005b9f) else Color.DarkGray,
            fontSize = 14.sp
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF005b9f))
            )
        }
    }
}