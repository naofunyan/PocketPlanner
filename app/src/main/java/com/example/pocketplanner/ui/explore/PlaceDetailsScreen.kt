package com.example.pocketplanner.ui.explore

import com.example.pocketplanner.data.repository.localized

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.pocketplanner.R
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
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(
    placeName: String,
    onNavigateBack: () -> Unit,
    onViewTrip: (String) -> Unit,
    onImmersiveExperienceClick: (String) -> Unit = {},
    viewModel: ItineraryViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel()
) {
    val savedNames by exploreViewModel.savedPlaces.collectAsState()

    val place = com.example.pocketplanner.data.repository.DestinationRepository.allSubplaces.find { it.name == placeName }?.localized()
        ?: com.example.pocketplanner.data.repository.Subplace(
            name = placeName, subtitle = String.format(androidx.compose.ui.res.stringResource(R.string.place_details_fallback_subtitle), placeName), description = androidx.compose.ui.res.stringResource(R.string.place_details_fallback_desc),
            countryName = androidx.compose.ui.res.stringResource(R.string.place_details_fallback_country), flagEmoji = "🌍", heroImageUrl = "https://picsum.photos/seed/${placeName.replace(" ", "")}hero/800/1000",
            mapImageUrl = "https://picsum.photos/seed/${placeName.replace(" ", "")}map/1000/1200", highlights = emptyList(),
            openingTime = androidx.compose.ui.res.stringResource(R.string.place_details_fallback_time), ticketPrice = androidx.compose.ui.res.stringResource(R.string.place_details_fallback_price), theme = androidx.compose.ui.res.stringResource(R.string.place_details_fallback_theme)
        )

    val subplaceCoordinates = mapOf(
        "The Independence Palace" to Point.fromLngLat(106.6953, 10.7770),
        "War Remnants Museum" to Point.fromLngLat(106.6924, 10.7794),
        "Cu Chi Tunnels" to Point.fromLngLat(106.4616, 11.1420),
        "Saigon Zoo & Botanical Gardens" to Point.fromLngLat(106.7054, 10.7877),
        "Banh Mi Huynh Hoa" to Point.fromLngLat(106.6917, 10.7712),
        "Hoa Lo Prison" to Point.fromLngLat(105.8464, 21.0252),
        "Ho Chi Minh Mausoleum" to Point.fromLngLat(105.8348, 21.0368),
        "St. Joseph's Cathedral" to Point.fromLngLat(105.8488, 21.0286),
        "Hoan Kiem Walking Street" to Point.fromLngLat(105.8523, 21.0285),
        "Bun Cha Huong Lien" to Point.fromLngLat(105.8546, 21.0163),
        "Ba Na Hills SunWorld" to Point.fromLngLat(107.9953, 15.9961),
        "Son Tra Beach" to Point.fromLngLat(108.2831, 16.1260),
        "Golden Bridge" to Point.fromLngLat(107.9942, 15.9950),
        "Hue Imperial City (The Citadel)" to Point.fromLngLat(107.5772, 16.4682),
        "Thien Mu Pagoda" to Point.fromLngLat(107.5451, 16.4534),
        "Lang Co Beach" to Point.fromLngLat(108.0683, 16.2483),
        "Bach Ma National Park" to Point.fromLngLat(107.8540, 16.1965)
    )
    val basePoint = subplaceCoordinates[place.name] ?: Point.fromLngLat(106.7009, 10.7769)
    // Shift camera south so place appears in top visible half
    val centerPoint = Point.fromLngLat(basePoint.longitude(), basePoint.latitude() - 0.008)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(centerPoint)
            zoom(6.0)
            pitch(0.0)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val initialPeekHeight = screenHeight * 0.52f

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    val trips by viewModel.trips.collectAsState()
    var showAddToPlanSheet by remember { mutableStateOf(false) }
    var selectedTripForPlan by remember { mutableStateOf<TripEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val placeDetailsAddedSnackbarTemplate = androidx.compose.ui.res.stringResource(R.string.place_details_added_snackbar)
    val placeDetailsViewActionText = androidx.compose.ui.res.stringResource(R.string.place_details_view_action)

    LaunchedEffect(place.name) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "test_user_id"
        viewModel.loadTrips(userId)

        kotlinx.coroutines.delay(400)
        mapViewportState.flyTo(
            com.mapbox.maps.CameraOptions.Builder()
                .center(centerPoint)
                .zoom(15.0)
                .pitch(60.0)
                .build(),
            com.mapbox.maps.plugin.animation.MapAnimationOptions.Builder().duration(2500).build()
        )
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
            sheetContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
            sheetShadowElevation = 12.dp,
            containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
            sheetDragHandle = null,

            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapboxMap(
                        Modifier.fillMaxSize(),
                        mapViewportState = mapViewportState,
                        mapInitOptionsFactory = { ctx ->
                            com.mapbox.maps.MapInitOptions(
                                context = ctx,
                                textureView = true,
                                styleUri = com.mapbox.maps.Style.MAPBOX_STREETS
                            )
                        }
                    )

                    // Add a top gradient to make the back/bookmark buttons pop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                                )
                            )
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
                            shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp, // DYNAMIC BACKGROUND
                            modifier = Modifier.size(44.dp).clickable { onNavigateBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, androidx.compose.ui.res.stringResource(R.string.place_details_back_cd), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) // DYNAMIC ICON
                            }
                        }

                        val isSaved = savedNames.contains(place.name)

                        Surface(
                            shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp, // DYNAMIC BACKGROUND
                            modifier = Modifier.size(44.dp).clickable {
                                exploreViewModel.toggleSavePlace(place.name, isSaved)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.place_details_bookmark_cd), tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp) // DYNAMIC ICON
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

                    val supportedImmersivePlaces = setOf(
                        "The Independence Palace",
                        "War Remnants Museum",
                        "Cu Chi Tunnels",
                        "Hoa Lo Prison",
                        "Hue Imperial City (The Citadel)",
                        "Thien Mu Pagoda",
                        "Bach Ma National Park"
                    )

                    if (place.name in supportedImmersivePlaces) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape, color = MaterialTheme.colorScheme.secondary, shadowElevation = 8.dp, modifier = Modifier.height(48.dp)
                                        .clip(CircleShape)
                                        .clickable { onImmersiveExperienceClick(place.name) }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(painter = painterResource(id = R.drawable.impression), null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(androidx.compose.ui.res.stringResource(R.string.place_details_immersive), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary))
                                    }
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
                containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                windowInsets = WindowInsets(0)
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {

                    if (selectedTripForPlan == null) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.place_details_add_to_which_trip),
                            color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )

                        if (trips.isEmpty()) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.place_details_no_trips),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(trips) { trip ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedTripForPlan = trip }.padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // DYNAMIC ICON
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(trip.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                                            Text(trip.destination, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) // DYNAMIC TEXT
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
                                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(R.string.place_details_back_cd),
                                tint = MaterialTheme.colorScheme.onSurface, // DYNAMIC ICON
                                modifier = Modifier.clickable { selectedTripForPlan = null }.padding(8.dp)
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.place_details_select_day), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        val durationDays = if (selectedTripForPlan!!.isOpenEnded) ((System.currentTimeMillis() - selectedTripForPlan!!.startDate) / 86400000).toInt().coerceAtLeast(0) + 1 else ((selectedTripForPlan!!.endDate - selectedTripForPlan!!.startDate) / 86400000).toInt() + 1

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
                                        
                                        // Hoisted strings for coroutine scope
                                        val addedMessage = String.format(placeDetailsAddedSnackbarTemplate, savedTripName, targetDay)
                                        val viewAction = placeDetailsViewActionText

                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = addedMessage, actionLabel = viewAction, duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) { onViewTrip(savedTripId) }
                                        }
                                    }.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(String.format(androidx.compose.ui.res.stringResource(R.string.place_details_day_format), targetDay, targetDate.format(formatter)), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
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
    val surfaceColor = MaterialTheme.colorScheme.surface // DYNAMIC SURFACE FOR GRADIENT FADE

    Box(modifier = Modifier.fillMaxWidth().height(340.dp + topPadding).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))) {
        AsyncImage(
            model = place.heroImageUrl, contentDescription = place.displayName,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.4f), 0.3f to Color.Transparent, 0.5f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.6f), 1f to Color.Black.copy(alpha = 0.1f))
        ))

        // DYNAMIC FADE OVERLAY
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to Color.Transparent, 0.85f to Color.Transparent, 0.95f to surfaceColor.copy(alpha = 0.8f), 1f to surfaceColor)
        ))

        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding + 12.dp).size(width = 38.dp, height = 4.dp).background(Color.White.copy(alpha = 0.8f), CircleShape))

        Row(modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = topPadding + 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(text = place.flagEmoji, fontSize = 24.sp) }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = place.countryName, color = Color.White, // Kept white to overlay on image properly
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 4f))
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = place.displayName, color = Color.White, textAlign = TextAlign.Center, // Kept white
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 2f), 12f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.subtitle.uppercase(), color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center, // Kept white
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, fontSize = 11.sp, shadow = Shadow(Color.Black.copy(alpha = 0.9f), Offset(0f, 2f), 12f))
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                onClick = onAddToPlan, shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 8.dp, modifier = Modifier.height(48.dp) // DYNAMIC BACKGROUND
            ) {
                Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) // DYNAMIC ICON
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.place_details_add_to_plan), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) // DYNAMIC TEXT
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
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 26.sp, fontSize = 16.sp) // DYNAMIC TEXT
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- NEW POLISHED INFO CARD ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Opening Hours Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Schedule, contentDescription = androidx.compose.ui.res.stringResource(R.string.place_details_time_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) // DYNAMIC ICON
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.place_details_opening_hours), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = place.openingTime, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp) // DYNAMIC DIVIDER

                // 2. Ticket Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = androidx.compose.ui.res.stringResource(R.string.place_details_price_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) // DYNAMIC ICON
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.place_details_price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = place.ticketPrice, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                    }
                }
            }
        }
    }
}