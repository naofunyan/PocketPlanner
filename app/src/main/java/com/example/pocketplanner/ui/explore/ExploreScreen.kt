package com.example.pocketplanner.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import coil.compose.AsyncImage
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Public

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onSearchClick: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 1. STATE FOR 3-STAGE SHEET: Tracks whether we are in the "Map Only" dropped-down state
    var isMapExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // 2. DYNAMIC PEEK HEIGHT: Smoothly animates between Tabs-Only (180.dp) and Half-Screen (55%)
    val targetPeekHeight = if (isMapExpanded) 240.dp else (screenHeight * 0.55f)
    val currentPeekHeight by animateDpAsState(
        targetValue = targetPeekHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // 1. Reusable drag logic
    val expandMapDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount > 15) { // Swiped down
                isMapExpanded = true
                coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
            } else if (dragAmount < -15) { // Swiped up
                isMapExpanded = false
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = currentPeekHeight,
        // 1. Give it a distinct top curve definition
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = Color.White,
        sheetShadowElevation = 8.dp, // Adds depth so the curve stands out from the map
        containerColor = Color.Transparent,

        sheetDragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(expandMapDragModifier) // 2. Attached here!
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },

        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/mapholder/800/1000",
                    contentDescription = "Map Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp)
                        .padding(horizontal = 24.dp)
                )
            }
        },

        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 3. Wrap the ENTIRE top header in our drag modifier!
                    // This turns the tabs and categories into a massive downward drag target.
                    Column(modifier = Modifier.fillMaxWidth().then(expandMapDragModifier)) {
                        TopSegmentedControl(
                            selectedIndex = selectedTabIndex,
                            onIndexSelected = { selectedTabIndex = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedTabIndex == 0) {
                            CategoryRow()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        if (selectedTabIndex == 0) {
                            item {
                                BrowseByCRegionSection()
                            }
                        } else {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
                                    Text("No saved destinations yet.", color = Color.Gray)
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
                    ExploreSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                }
            }
        }
    )
}

@Composable
fun TopSegmentedControl(
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Gap between the buttons
    ) {
        TabItem(
            text = "Discover",
            isSelected = selectedIndex == 0,
            onClick = { onIndexSelected(0) },
            modifier = Modifier.weight(1f)
        )

        TabItem(
            text = "Saved",
            isSelected = selectedIndex == 1,
            onClick = { onIndexSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF092A3A) else Color(0xFFE2E2E6), // Navy vs Light Gray
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF092A3A),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun CategoryRow() {
    val categories = listOf(
        Pair("🌲", "Nature"),
        Pair("🧗", "Adventure"),
        Pair("🏛️", "Culture"),
        Pair("🍴", "Food"),
        Pair("🐾", "Wildlife"),
        Pair("🏙️", "City"),
        Pair("🏖️", "Beach")
    )

    var selectedCategory by remember { mutableStateOf("Nature") }

    Column {
        Text(
            text = "Browse by travel theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A4B),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Swapped FlowRow back to LazyRow for a single, swipeable horizontal line!
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat.second

                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFFF0E6FA) else Color.White,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { selectedCategory = cat.second }
                ) {
                    Row(
                        // Slightly tighter padding to make the chips themselves more compact
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat.first, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.second,
                            color = if (isSelected) Color(0xFF6B4FA9) else Color(0xFF1E3A4B),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreCard(destination: Destination, onNavigateToDetails: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { onNavigateToDetails(destination.id) },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
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

                if (destination.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE65100).copy(alpha = 0.9f),
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
                        onClick = { onNavigateToDetails(destination.id) }, // <--- WE TRIGGER THE NAVIGATION HERE
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search destinations...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class CRegion(val name: String, val color: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseByCRegionSection() {
    val regions = listOf(
        CRegion("Northeast", Color(0xFFF0904E)),
        CRegion("Northwest", Color(0xFF26A8F0)),
        CRegion("Red River Delta", Color(0xFFEE6384)),
        CRegion("North Central Coast", Color(0xFFDA5C43)),
        CRegion("South Central Coast", Color(0xFFA5A292)),
        CRegion("Central Highlands", Color(0xFFBE7898)),
        CRegion("Southeast", Color(0xFFBE7898)), // Matched your screenshot colors/layout
        CRegion("Mekong River Delta", Color(0xFFBE7898))
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = "Browse by subregion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A4B)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Replaced FlowRow with a strictly chunked loop to enforce perfect grid sizing
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            regions.chunked(2).forEach { rowRegions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowRegions.forEach { region ->
                        CRegionCard(region = region, modifier = Modifier.weight(1f))
                    }

                    // 2. If a row only has 1 item, add an invisible spacer to keep the column width locked!
                    if (rowRegions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CRegionCard(region: CRegion, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF4F6F9),
        modifier = modifier
            .height(76.dp) // 3. Increased height slightly to accommodate 3 lines of text
            .clickable { /* TODO: Navigate to region filter */ }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = region.name,
                style = MaterialTheme.typography.labelMedium, // 4. Slightly smaller font prevents ugly character breaks
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A4B),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp) // 5. Tighter padding maximizes horizontal room for text
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .offset(x = 16.dp)
                    .background(region.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp)
                )
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