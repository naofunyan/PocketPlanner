package com.example.pocketplanner.ui.explore

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.geojson.Point

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onSearchClick: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val savedNames by viewModel.savedPlaces.collectAsState()

    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var isMapExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val vietnamCenter = Point.fromLngLat(108.2772, 14.0583)
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(vietnamCenter)
            zoom(0.0) // Start completely zoomed out
            pitch(0.0)
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        mapViewportState.flyTo(
            com.mapbox.maps.CameraOptions.Builder()
                .center(vietnamCenter)
                .zoom(4.5)
                .build(),
            com.mapbox.maps.plugin.animation.MapAnimationOptions.Builder().duration(3000).build()
        )
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val targetPeekHeight = if (isMapExpanded) 240.dp else (screenHeight * 0.55f)
    val currentPeekHeight by animateDpAsState(
        targetValue = targetPeekHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    val expandMapDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount > 15) {
                isMapExpanded = true
                coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
            } else if (dragAmount < -15) {
                isMapExpanded = false
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = currentPeekHeight,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
        sheetShadowElevation = 8.dp,
        containerColor = Color.Transparent,
        sheetDragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(expandMapDragModifier)
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                // Add a top gradient to make the "Explore" text pop against the map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White, // Kept white for image overlay
                    modifier = Modifier
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp)
                        .padding(horizontal = 24.dp)
                )
            }
        },
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {

                    Column(modifier = Modifier.fillMaxWidth().then(expandMapDragModifier)) {
                        TopSegmentedControl(
                            selectedIndex = selectedTabIndex,
                            onIndexSelected = { selectedTabIndex = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedTabIndex == 0) {
                            CategoryRow(
                                selectedCategory = selectedCategory,
                                onCategoryClick = { clickedCat ->
                                    selectedCategory = if (selectedCategory == clickedCat) null else clickedCat
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 200.dp)
                    ) {
                        if (selectedTabIndex == 0) {
                            if (searchQuery.isNotBlank()) {
                                item {
                                    SearchResultsSection(
                                        query = searchQuery,
                                        selectedCategory = selectedCategory,
                                        onPlaceClick = onPlaceClick
                                    )
                                }
                            } else {
                                item {
                                    ForYouSection(
                                        selectedCategory = selectedCategory,
                                        onNavigateToDetails = onNavigateToDetails
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(32.dp)) }

                                item {
                                    UnderTheRadarSection(
                                        selectedCategory = selectedCategory,
                                        onPlaceClick = onPlaceClick
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(32.dp)) }
                            }
                        } else {
                            val savedDestinations = savedNames.map { savedTitle ->
                                com.example.pocketplanner.data.repository.DestinationRepository.allSubplaces.find { it.name == savedTitle }
                                    ?: com.example.pocketplanner.data.repository.Subplace(
                                        name = savedTitle, subtitle = "Saved destination", description = "",
                                        countryName = "Global", flagEmoji = "🌍",
                                        heroImageUrl = "https://picsum.photos/seed/${savedTitle.replace(" ", "")}/800/1000",
                                        mapImageUrl = "", highlights = emptyList(), openingTime = "", ticketPrice = "", theme = "Saved"
                                    )
                            }

                            if (savedDestinations.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(64.dp).padding(bottom = 16.dp)) // DYNAMIC
                                        Text("No saved destinations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                                    }
                                }
                            } else {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                                        savedDestinations.forEach { dest ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onPlaceClick(dest.name) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = dest.heroImageUrl,
                                                        contentDescription = dest.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = dest.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(text = dest.flagEmoji, fontSize = 14.sp)
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "View",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // DYNAMIC
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val isImeVisible = WindowInsets.isImeVisible
                val bottomPadding = if (isImeVisible) 16.dp else 110.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = bottomPadding)
                        .imePadding()
                ) {
                    ExploreSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearchSubmit = { }
                    )
                }
            }
        }
    )
}

@Composable
fun TopSegmentedControl(selectedIndex: Int, onIndexSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabItem("Discover", selectedIndex == 0, { onIndexSelected(0) }, Modifier.weight(1f))
        TabItem("Saved", selectedIndex == 1, { onIndexSelected(1) }, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun CategoryRow(selectedCategory: String?, onCategoryClick: (String) -> Unit) {
    val categories = listOf(
        Pair("🌲", "Nature"), Pair("🏛️", "Culture"), Pair("🍴", "Food"),
        Pair("🏙️", "City"), Pair("🏖️", "Beach")
    )

    Column {
        Text(
            text = "Browse by travel theme", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat.second
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, // DYNAMIC
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null, // DYNAMIC
                    modifier = Modifier.clip(CircleShape).clickable { onCategoryClick(cat.second) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat.first, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.second,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, // DYNAMIC
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, // DYNAMIC BACKGROUND
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query, onValueChange = onQueryChange,
                placeholder = { Text("Search destinations...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }, // DYNAMIC TEXT
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary, // DYNAMIC CURSOR
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearchSubmit()
                    keyboardController?.hide()
                }),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = CircleShape, color = MaterialTheme.colorScheme.primary, // DYNAMIC BUTTON
                modifier = Modifier.size(36.dp).clickable {
                    onSearchSubmit()
                    keyboardController?.hide()
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Go", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp)) // DYNAMIC ICON
                }
            }
        }
    }
}

@Composable
fun ForYouSection(
    selectedCategory: String?,
    onNavigateToDetails: (String) -> Unit = {}
) {
    var cities = com.example.pocketplanner.data.repository.DestinationRepository.cities

    if (selectedCategory != null) {
        cities = cities.filter { it.theme == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "For you", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (cities.isEmpty()) {
            Text(
                text = "No destinations found.", color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cities) { city ->
                    ForYouCard(city = city, onNavigateToDetails = onNavigateToDetails)
                }
            }
        }
    }
}

@Composable
fun ForYouCard(
    city: com.example.pocketplanner.data.repository.City,
    onNavigateToDetails: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.width(280.dp).height(200.dp).clickable { onNavigateToDetails(city.title) },
        shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = city.heroImageUrl, contentDescription = city.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent, 0.3f to Color.Transparent,
                        0.6f to Color.Black.copy(alpha = 0.4f), 1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            Box(
                modifier = Modifier.padding(16.dp).size(32.dp).background(Color.White.copy(alpha = 0.2f), CircleShape).align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(text = city.flagEmoji, fontSize = 18.sp)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = city.title, color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(2f, 2f), 8f)
                        )
                    )
                    Text(text = "${city.topPlaces.size} Places", color = Color(0xFFE0E0E0), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun UnderTheRadarSection(
    selectedCategory: String?,
    onPlaceClick: (String) -> Unit = {}
) {
    var places = com.example.pocketplanner.data.repository.DestinationRepository.allSubplaces

    if (selectedCategory != null) {
        places = places.filter { it.theme == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Under the radar", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (places.isEmpty()) {
            Text(
                text = "No hidden gems found.", color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(places) { place ->
                    UnderTheRadarCard(subplace = place, onPlaceClick = onPlaceClick)
                }
            }
        }
    }
}

@Composable
fun UnderTheRadarCard(
    subplace: com.example.pocketplanner.data.repository.Subplace,
    onPlaceClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.width(160.dp).height(160.dp).clickable { onPlaceClick(subplace.name) },
        shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = subplace.heroImageUrl, contentDescription = subplace.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(text = subplace.flagEmoji, fontSize = 14.sp) }
                }
            }
            Text(
                text = subplace.name,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(2f, 2f), 8f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun SearchResultsSection(
    query: String,
    selectedCategory: String?,
    onPlaceClick: (String) -> Unit
) {
    var places = com.example.pocketplanner.data.repository.DestinationRepository.allSubplaces

    if (selectedCategory != null) places = places.filter { it.theme == selectedCategory }
    places = places.filter { it.name.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = "Search Results", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (places.isEmpty()) {
            Text(text = "No destinations found for \"$query\".", color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
        } else {
            places.forEach { dest ->
                Surface(
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onPlaceClick(dest.name) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = dest.heroImageUrl, contentDescription = dest.name,
                            contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = dest.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = dest.flagEmoji, fontSize = 14.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) // DYNAMIC ICON
                    }
                }
            }
        }
    }
}