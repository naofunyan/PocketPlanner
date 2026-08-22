package com.example.pocketplanner.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreDetailsScreen(
    destinationId: String,
    onNavigateBack: () -> Unit,
    onCreateNewTrip: () -> Unit = {},
    onSeeAllPlaces: (String) -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    // 1. INJECT THE VIEWMODEL HERE
    viewModel: ExploreViewModel = hiltViewModel()
) {
    // 2. COLLECT THE DATABASE STATE
    val savedNames by viewModel.savedPlaces.collectAsState()

    // 1. PULL REAL DATA FROM REPOSITORY! (Fallback to first city if not found)
    val destination = com.example.pocketplanner.data.repository.DestinationRepository.cities.find { it.id == destinationId }
        ?: com.example.pocketplanner.data.repository.DestinationRepository.cities.first()

    // 2. Generate a dynamic map image using the city ID
    val mapImageUrl = "https://picsum.photos/seed/${destination.id.replace(" ", "")}map/1000/1200"

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val initialPeekHeight = screenHeight * 0.52f

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = initialPeekHeight,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetContainerColor = Color.White,
        sheetShadowElevation = 12.dp,
        containerColor = Color.Black,
        sheetDragHandle = null,

        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = mapImageUrl,
                    contentDescription = "Map View",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    Surface(
                        shape = CircleShape, color = Color.White, shadowElevation = 6.dp,
                        modifier = Modifier.size(44.dp).clickable { onNavigateBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1E3A4B), modifier = Modifier.size(22.dp))
                        }
                    }

                    // 3. CHECK THE VIEWMODEL STATE
                    val isSaved = savedNames.contains(destination.title)

                    Surface(
                        shape = CircleShape, color = Color.White, shadowElevation = 6.dp,
                        modifier = Modifier.size(44.dp).clickable {
                            // 4. USE THE VIEWMODEL FUNCTION TO SAVE/DELETE FROM ROOM DATABASE
                            viewModel.toggleSavePlace(destination.title, isSaved)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = Color(0xFF1E3A4B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        sheetContent = {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                item {
                    DestinationHeroBanner(destination = destination, onCreateNewTrip = onCreateNewTrip)
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            text = destination.description,
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF4A5568), lineHeight = 26.sp, fontSize = 16.sp)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    DestinationPlacesSection(
                        destinationId = destination.id,
                        places = destination.topPlaces,
                        onSeeAllPlaces = onSeeAllPlaces,
                        onPlaceClick = onPlaceClick
                    )
                }
            }
        }
    )
}

@Composable
fun DestinationPlacesSection(
    destinationId: String,
    places: List<com.example.pocketplanner.data.repository.Subplace>, // Accepts real Subplaces
    onSeeAllPlaces: (String) -> Unit,
    onPlaceClick: (String) -> Unit = {}
) {
    if (places.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(text = "Places", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A4B))
        Spacer(modifier = Modifier.height(20.dp))

        places.forEach { place ->
            PlaceListItem(place = place, onPlaceClick = onPlaceClick)
            Spacer(modifier = Modifier.height(20.dp))
        }

        OutlinedButton(
            onClick = { onSeeAllPlaces(destinationId) },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).height(48.dp),
            shape = CircleShape, border = BorderStroke(1.dp, Color(0xFFE2E2E6)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
        ) {
            Text(text = "See all", color = Color(0xFF1E3A4B), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun PlaceListItem(
    place: com.example.pocketplanner.data.repository.Subplace, // Accepts real Subplace
    onPlaceClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onPlaceClick(place.name) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF4F6F9))
        ) {
            AsyncImage(
                // Mapped to the new heroImageUrl
                model = place.heroImageUrl,
                contentDescription = place.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = place.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = Color(0xFF1E3A4B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = place.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A869A), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DestinationHeroBanner(
    destination: com.example.pocketplanner.data.repository.City, // Accepts real City
    onCreateNewTrip: () -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxWidth().height(340.dp + topPadding).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))) {
        AsyncImage(
            model = destination.heroImageUrl, contentDescription = destination.title,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0.0f to Color.Black.copy(alpha = 0.4f), 0.3f to Color.Transparent, 0.5f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.6f), 1.0f to Color.Black.copy(alpha = 0.1f))
        ))
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0.0f to Color.Transparent, 0.85f to Color.Transparent, 0.95f to Color.White.copy(alpha = 0.8f), 1.0f to Color.White)
        ))
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding + 12.dp).size(width = 38.dp, height = 4.dp).background(Color.White.copy(alpha = 0.8f), CircleShape))

        Surface(
            shape = CircleShape, color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = topPadding + 16.dp).size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Text(text = destination.flagEmoji, fontSize = 20.sp) }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = destination.title, color = Color.White, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), offset = Offset(0f, 2f), blurRadius = 12f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = destination.tagline.uppercase(), color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, fontSize = 11.sp, shadow = Shadow(color = Color.Black.copy(alpha = 0.9f), offset = Offset(0f, 2f), blurRadius = 12f))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                onClick = onCreateNewTrip, shape = CircleShape, color = Color.White, shadowElevation = 8.dp,
                modifier = Modifier.height(48.dp).shadow(elevation = 8.dp, shape = CircleShape)
            ) {
                Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Add, null, tint = Color(0xFF092A3A), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create a new trip", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF092A3A)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPlacesScreen(
    destinationId: String,
    onNavigateBack: () -> Unit,
    onPlaceClick: (String) -> Unit = {}
) {
    // 1. FETCH FROM REAL REPOSITORY
    val destination = com.example.pocketplanner.data.repository.DestinationRepository.cities.find { it.id == destinationId }
        ?: com.example.pocketplanner.data.repository.DestinationRepository.cities.first()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Places", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF092A3A))
                        Text(text = destination.title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A5568))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate Back", tint = Color(0xFF092A3A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, scrolledContainerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(destination.topPlaces) { place ->
                PlaceListItem(place = place, onPlaceClick = onPlaceClick)
            }
        }
    }
}