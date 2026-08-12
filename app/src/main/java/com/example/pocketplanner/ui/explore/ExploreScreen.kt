package com.example.pocketplanner.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onSearchClick: () -> Unit = {}) {
    val listState = rememberLazyListState()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. The Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 24.dp, bottom = 180.dp) // Large bottom padding for floating bar + nav pill
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        TopTogglePill()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                item {
                    CategoryRow()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(mockDestinations) { dest ->
                    ExploreCard(destination = dest)
                }
            }

            // 2. The Floating Search Bar at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    // The top edge of the floating pill is exactly 96dp from the bottom (24dp padding + 72dp pill).
                    // We set padding to 104dp so the search bar sits just 8dp above the navbar pill.
                    .padding(bottom = 104.dp)
            ) {
                FloatingSearchBar(onClick = onSearchClick)
            }
        }
    }
}

@Composable
fun TopTogglePill() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = CircleShape,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            // Discover (Selected)
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Text(
                    text = "Discover",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            // Saved (Unselected)
            Text(
                text = "Saved",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun CategoryRow() {
    val categories = listOf(
        Pair("⛰️", "Nature"),
        Pair("🏂", "Adventure"),
        Pair("🏙️", "City"),
        Pair("🏖️", "Beach")
    )
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories.size) { index ->
            val cat = categories[index]
            val isSelected = index == 0
            
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = cat.first)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.second, 
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreCard(destination: Destination) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Top Image Half
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = destination.imageUrl,
                    contentDescription = destination.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating Heart Icon
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Trending Badge
                if (destination.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE65100).copy(alpha = 0.9f), // Orange
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = destination.badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Text Half
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    // Star Rating
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(destination.rating, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = destination.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "From ${destination.price}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / day",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "View Details",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingSearchBar(onClick: () -> Unit = {}) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Search destinations...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            
            // Blue circular arrow button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class Destination(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val rating: String,
    val imageUrl: String,
    val badge: String? = null
)
