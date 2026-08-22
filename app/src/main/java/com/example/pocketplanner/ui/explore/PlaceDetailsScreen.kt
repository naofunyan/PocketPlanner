package com.example.pocketplanner.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.ui.itinerary.ItineraryViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(
    placeName: String,
    onNavigateBack: () -> Unit,
    onViewTrip: (String) -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel(),
    // 1. INJECT THE EXPLORE VIEWMODEL HERE
    exploreViewModel: ExploreViewModel = hiltViewModel()
) {
    // 2. COLLECT THE DATABASE STATE
    val savedNames by exploreViewModel.savedPlaces.collectAsState()

    // 1. FETCH FROM REAL REPOSITORY!
    val place = com.example.pocketplanner.data.repository.DestinationRepository.allSubplaces.find { it.name == placeName }
    // Fallback generator just in case a name gets passed that isn't in the database yet
        ?: com.example.pocketplanner.data.repository.Subplace(
            name = placeName, subtitle = "Discover $placeName", description = "No description available yet.",
            countryName = "Global", flagEmoji = "🌍", heroImageUrl = "https://picsum.photos/seed/${placeName.replace(" ", "")}hero/800/1000",
            mapImageUrl = "https://picsum.photos/seed/${placeName.replace(" ", "")}map/1000/1200", highlights = emptyList(),
            openingTime = "Check local times", ticketPrice = "Varies", theme = "Saved"
        )

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val initialPeekHeight = screenHeight * 0.52f

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // State for the Add to Plan Flow
    val trips by viewModel.trips.collectAsState()
    var showAddToPlanSheet by remember { mutableStateOf(false) }
    var selectedTripForPlan by remember { mutableStateOf<TripEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "test_user_id"
        viewModel.loadTrips(userId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        BottomSheetScaffold(
            modifier = Modifier.padding(paddingValues),
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
                        model = place.mapImageUrl,
                        contentDescription = "Map View",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                                start = 20.dp, end = 20.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape, color = Color.White, shadowElevation = 6.dp,
                            modifier = Modifier.size(44.dp).clickable { onNavigateBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1E3A4B), modifier = Modifier.size(22.dp))
                            }
                        }

                        // 3. CHECK THE VIEWMODEL STATE INSTEAD OF THE OLD MEMORY LIST
                        val isSaved = savedNames.contains(place.name)

                        Surface(
                            shape = CircleShape, color = Color.White, shadowElevation = 6.dp,
                            modifier = Modifier.size(44.dp).clickable {
                                // 4. USE THE VIEWMODEL FUNCTION TO SAVE/DELETE FROM ROOM DATABASE
                                exploreViewModel.toggleSavePlace(place.name, isSaved)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark", tint = Color(0xFF1E3A4B), modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            },

            sheetContent = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        PlaceHeroBanner(
                            place = place,
                            onAddToPlan = { showAddToPlanSheet = true }
                        )
                    }

                    if (place.highlights.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(place.highlights) { highlight ->
                                    PlaceHighlightCard(highlight = highlight)
                                }
                            }
                        }
                    }

                    item {
                        PlaceDescriptionSection(place = place)
                    }
                }
            }
        )

        // --- ADD TO PLAN MULTI-STEP BOTTOM SHEET ---
        if (showAddToPlanSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddToPlanSheet = false
                    selectedTripForPlan = null
                },
                containerColor = Color.White,
                windowInsets = WindowInsets(0)
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {

                    if (selectedTripForPlan == null) {
                        Text(
                            text = "Add to which trip?",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )

                        if (trips.isEmpty()) {
                            Text(
                                text = "You don't have any upcoming trips yet.",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(trips) { trip ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedTripForPlan = trip }.padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF092A3A))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(trip.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(trip.destination, color = Color.Gray, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                                modifier = Modifier.clickable { selectedTripForPlan = null }.padding(8.dp)
                            )
                            Text(
                                text = "Select Day", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        val durationDays = ((selectedTripForPlan!!.endDate - selectedTripForPlan!!.startDate) / 86400000).toInt() + 1

                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(durationDays) { index ->
                                val targetDay = index + 1
                                val instant = Instant.ofEpochMilli(selectedTripForPlan!!.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                val targetDate = instant.plusDays((targetDay - 1).toLong())
                                val formatter = DateTimeFormatter.ofPattern("MMM d")

                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        viewModel.addDetailedPlaceToDay(
                                            tripId = selectedTripForPlan!!.id,
                                            dayNumber = targetDay, name = place.name,
                                            category = "Sightseeing", photoUrl = place.heroImageUrl
                                        )
                                        showAddToPlanSheet = false
                                        val savedTripId = selectedTripForPlan!!.id
                                        val savedTripName = selectedTripForPlan!!.name
                                        selectedTripForPlan = null

                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Added to $savedTripName, Day $targetDay", actionLabel = "View", duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) { onViewTrip(savedTripId) }
                                        }
                                    }.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Day $targetDay - ${targetDate.format(formatter)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceHeroBanner(place: com.example.pocketplanner.data.repository.Subplace, onAddToPlan: () -> Unit) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxWidth().height(340.dp + topPadding).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))) {
        AsyncImage(
            model = place.heroImageUrl, contentDescription = place.name,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.4f), 0.3f to Color.Transparent, 0.5f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.6f), 1f to Color.Black.copy(alpha = 0.1f))
        ))
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to Color.Transparent, 0.85f to Color.Transparent, 0.95f to Color.White.copy(alpha = 0.8f), 1f to Color.White)
        ))
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding + 12.dp).size(width = 38.dp, height = 4.dp).background(Color.White.copy(alpha = 0.8f), CircleShape))

        Row(modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = topPadding + 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(text = place.flagEmoji, fontSize = 24.sp) }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = place.countryName, color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 4f))
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = place.name, color = Color.White, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 2f), 12f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.subtitle.uppercase(), color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, fontSize = 11.sp, shadow = Shadow(Color.Black.copy(alpha = 0.9f), Offset(0f, 2f), 12f))
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                onClick = onAddToPlan, shape = CircleShape, color = Color.White, shadowElevation = 8.dp, modifier = Modifier.height(48.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Add, null, tint = Color(0xFF092A3A), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to plan", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF092A3A)))
                }
            }
        }
    }
}

@Composable
fun PlaceHighlightCard(highlight: com.example.pocketplanner.data.repository.PlaceHighlight) {
    Box(modifier = Modifier.width(170.dp).height(180.dp)) {
        Card(
            modifier = Modifier.width(155.dp).height(160.dp).align(Alignment.BottomEnd),
            shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(model = highlight.imageUrl, contentDescription = highlight.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Surface(
            color = highlight.tagColor, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.TopStart).rotate(-4f)
        ) {
            Text(
                text = highlight.title, color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PlaceDescriptionSection(place: com.example.pocketplanner.data.repository.Subplace) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Text(
            text = place.description,
            style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF4A5568), lineHeight = 26.sp, fontSize = 16.sp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- NEW POLISHED INFO CARD ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF4F6F9), // Soft slate background
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp), // Generous padding inside the box
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Opening Hours Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White circular background for the icon
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Schedule, contentDescription = "Time", tint = Color(0xFF092A3A), modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // Text takes up the rest of the horizontal space
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Opening Hours", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7A869A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = place.openingTime, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF092A3A))
                    }
                }

                // A subtle divider line between the two items
                Divider(color = Color(0xFFE2E2E6), thickness = 1.dp)

                // 2. Ticket Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White circular background for the icon
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = "Price", tint = Color(0xFF092A3A), modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // Text takes up the rest of the horizontal space
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Price", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7A869A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = place.ticketPrice, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF092A3A))
                    }
                }
            }
        }
    }
}