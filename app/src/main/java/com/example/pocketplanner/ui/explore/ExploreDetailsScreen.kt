package com.example.pocketplanner.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreDetailsScreen(
    destinationId: String,
    onNavigateBack: () -> Unit,
    onAddToTrip: () -> Unit
) {
    val destination = getMockDestinationById(destinationId)

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Price", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(
                            text = destination.price,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = onAddToTrip,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .padding(start = 24.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add to Trip", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Background Image Header
            AsyncImage(
                model = destination.imageUrl,
                contentDescription = destination.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Bottom Content Sheet
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 350.dp), // Pushed down to overlap the image
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Badge and Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (destination.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE65100).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = destination.badge,
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(destination.rating, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = destination.description + " This beautiful location offers breathtaking views, unique cultural experiences, and unforgettable memories. Perfect for travelers seeking both adventure and relaxation.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

// Temporary Helper to share the mock data
fun getMockDestinationById(id: String): Destination {
    val mockDestinations = listOf(
        Destination(
            id = "1",
            title = "Grand Canyon",
            description = "Experience the breathtaking scale and vibrant colors of one of the world's most renowned natural wonders.",
            price = "$120",
            rating = "4.9",
            imageUrl = "https://picsum.photos/seed/grandcanyon/800/400",
            badge = "🔥 Trending"
        ),
        Destination(
            id = "2",
            title = "Tokyo City",
            description = "Dive into a vibrant metropolis blending neon-lit skyscrapers with historic temples, offering endless discoveries.",
            price = "$200",
            rating = "4.8",
            imageUrl = "https://picsum.photos/seed/tokyo/800/400"
        ),
        Destination(
            id = "3",
            title = "Maldives Resort",
            description = "Relax in luxury overwater bungalows surrounded by crystal clear turquoise waters and pristine white beaches.",
            price = "$450",
            rating = "5.0",
            imageUrl = "https://picsum.photos/seed/maldives/800/400"
        )
    )
    return mockDestinations.find { it.id == id } ?: mockDestinations.first()
}