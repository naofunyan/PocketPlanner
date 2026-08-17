package com.example.pocketplanner.ui.tracking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun TrackingScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onPlanClick: () -> Unit,
    onExpenseClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full Screen Map Background Placeholder
        Image(
            painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=1080&auto=format&fit=crop"), 
            // Using a generic map-like satellite image from Unsplash as placeholder
            contentDescription = "Satellite Map Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark gradient overlay to make text readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        
        // 2. Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding + 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Back Button Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.clickable(onClick = onNavigateBack)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = Color(0xFF005b9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", color = Color(0xFF005b9f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // (Only back button remains in this row)
        }

        // 3. Center Map Overlay (Route Pin)
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        "Add step",
                        color = Color(0xFF01579B), // Dark blue text
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // Arrow pointing down from pill
                Box(modifier = Modifier.size(12.dp)) {
                    // Custom draw triangle or just simple layout, omit for brevity
                }
                
                // Blue Marker Dot
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF29B6F6),
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.padding(2.dp))
                }
                
                // Mock route line text
                Spacer(modifier = Modifier.height(32.dp))
                Text("N/A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Bottom Area Wrapper
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // 4. Horizontal Cards Carousel
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trip Started Card
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0F1722).copy(alpha = 0.95f),
                        modifier = Modifier
                            .width(160.dp)
                            .height(180.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color(0xFF0F1722), modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Trip started", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("SAT 8 AUG 2026", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            // Red Blinking Dot Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFFE91E63), CircleShape)
                            )
                        }
                    }
                }
                
                // Plus button separator
                item {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.padding(4.dp))
                    }
                }

                // Ho Chi Minh City Card
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0F1722).copy(alpha = 0.95f),
                        modifier = Modifier
                            .width(280.dp)
                            .height(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Suggested Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E2A38),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Suggested", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text("N/A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Trash icon
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E2A38),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(8.dp))
                                }
                                
                                // Add Step Button
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp)
                                        .height(40.dp)
                                        .clickable { }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF0F1722), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add step", color = Color(0xFF0F1722), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Custom Bottom Nav Pill (Dark version)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        BottomNavPill("Plan", false) { onPlanClick() }
                        BottomNavPill("Expense", false) { onExpenseClick() }
                        BottomNavPill("Track", true) { }
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
