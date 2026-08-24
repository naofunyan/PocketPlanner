package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import androidx.compose.ui.draw.clip
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
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.Style
import com.mapbox.maps.CameraOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.pocketplanner.BuildConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ItineraryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onDayClick: (dayNumber: Int) -> Unit,
    onExpenseClick: () -> Unit,
    viewModel: ItineraryViewModel = hiltViewModel()
) {
    val trip by viewModel.getTrip(tripId).collectAsState(initial = null)

    var selectedDay by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(1) }
    val places by viewModel.getPlacesForDay(tripId, selectedDay).collectAsState(initial = emptyList())
    val allPlaces by viewModel.getAllPlaces(tripId).collectAsState(initial = emptyList())
    var showRearrangeSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    var placeMenuTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.pocketplanner.data.local.entity.PlaceEntity?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(selectedDay) {
        listState.scrollToItem(0)
    }

    val dayColors = listOf(
        "#81D4FA", // Day 1: Cyan
        "#FFAB91", // Day 2: Peach
        "#B39DDB", // Day 3: Purple
        "#A5D6A7", // Day 4: Green
        "#F48FB1", // Day 5: Pink
        "#FFE082", // Day 6: Yellow
        "#90CAF9"  // Day 7: Blue
    )
    val currentDayColorHex = dayColors[(selectedDay - 1).coerceAtLeast(0) % dayColors.size]


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.enableProximityAlerts(tripId)
        }
    }

    // Background Mapbox Map (Top half)
    var cachedLng by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    var cachedLat by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Double?>(null) }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            if (cachedLng != null && cachedLat != null) {
                center(Point.fromLngLat(cachedLng!!, cachedLat!!))
                zoom(12.0)
            } else {
                zoom(2.0) // default world zoom
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsState()

    val routeLegs by viewModel.routeLegs.collectAsState()
    val placePhotos by viewModel.placePhotos.collectAsState()

    LaunchedEffect(places) {
        if (places.size >= 2) {
            viewModel.fetchRouteForDay(places)
        }
    }
    val selectedPlaceDetails by viewModel.selectedPlaceDetails.collectAsState()
    val isFetchingPlaceDetails by viewModel.isFetchingPlaceDetails.collectAsState()

    val dateFormatter = SimpleDateFormat("EEE M/d", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background // DYNAMIC BACKGROUND
    ) { innerPadding ->
        if (trip == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // DYNAMIC COLOR
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                var showCalendarSheet by remember { mutableStateOf(false) }

                val context = LocalContext.current
                val density = LocalDensity.current.density
                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val peekHeightPx = (screenHeight.value * 0.6f * density).toDouble()

                LaunchedEffect(trip?.destination, places) {
                    if (places.isNotEmpty()) {
                        val avgLat = places.map { it.lat }.average()
                        val avgLng = places.map { it.lng }.average()
                        cachedLat = avgLat
                        cachedLng = avgLng
                        mapViewportState.setCameraOptions(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(avgLng, avgLat))
                                .padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, peekHeightPx, 0.0))
                                .zoom(13.5)
                                .build()
                        )
                    } else if (cachedLng == null || cachedLat == null) {
                        trip?.destination?.let { dest ->
                            withContext<Unit>(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = try { geocoder.getFromLocationName(dest, 1) } catch(e: Exception) { null }
                                    var lat: Double? = null
                                    var lng: Double? = null

                                    if (!addresses.isNullOrEmpty()) {
                                        lat = addresses[0].latitude
                                        lng = addresses[0].longitude
                                    } else {
                                        val geo = viewModel.geocodeCity(dest)
                                        if (geo != null) {
                                            lat = geo.first
                                            lng = geo.second
                                        }
                                    }

                                    if (lat != null && lng != null) {
                                        cachedLng = lng
                                        cachedLat = lat

                                        val pt = Point.fromLngLat(lng, lat)
                                        withContext<Unit>(Dispatchers.Main) {
                                            mapViewportState.setCameraOptions(
                                                CameraOptions.Builder()
                                                    .center(pt)
                                                    .padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, peekHeightPx, 0.0))
                                                    .zoom(12.0)
                                                    .build()
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                val sheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.PartiallyExpanded,
                    skipHiddenState = true
                )
                val scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = sheetState
                )

                val peekHeight = screenHeight * 0.6f
                val maxExpandedHeight = screenHeight - (innerPadding.calculateTopPadding() + 64.dp)

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = peekHeight,
                    sheetContainerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
                    sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    sheetDragHandle = null,
                    modifier = Modifier.fillMaxSize(),
                    sheetContent = {
                        Column(
                            modifier = Modifier.height(maxExpandedHeight).fillMaxWidth()
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
                                        .background(MaterialTheme.colorScheme.outlineVariant) // DYNAMIC COLOR
                                )
                            }

                            val diffInMillies = Math.abs(trip!!.endDate - trip!!.startDate)
                            val daysCount = (diffInMillies / 86400000L).toInt() + 1
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
                                        color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                                        modifier = Modifier.size(40.dp).clickable { showCalendarSheet = true }
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = com.example.pocketplanner.R.drawable.ntcalendar),
                                            contentDescription = "Calendar",
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                items(totalDays) { index ->
                                    val dayNum = index + 1
                                    val isSelected = dayNum == selectedDay

                                    val calendar = Calendar.getInstance()
                                    calendar.timeInMillis = trip!!.startDate
                                    calendar.add(Calendar.DAY_OF_YEAR, index)
                                    val dateString = dateFormatter.format(calendar.time)

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null, // DYNAMIC BORDER
                                        modifier = Modifier.clickable { selectedDay = dayNum }
                                    ) {
                                        Text(
                                            text = dateString,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // DYNAMIC DIVIDER

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
                                    Text(currentDateStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) // DYNAMIC TEXT
                                }
                            }

                            // Timeline Places List
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 120.dp)
                            ) {
                                if (places.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("No places added for this day yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                                        }
                                    }
                                } else {
                                    itemsIndexed(places) { index, place ->
                                        val isLastItem = index == places.lastIndex

                                        Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) { // DYNAMIC BACKGROUND
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(IntrinsicSize.Min)
                                            ) {
                                                // Left Timeline Column
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.width(40.dp)
                                                ) {
                                                    // Circular Badge (Uses Native Graphics Color parsed from Hex for consistency with map pins)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(Color(android.graphics.Color.parseColor(currentDayColorHex)), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}",
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 14.sp
                                                        )
                                                    }

                                                    if (!isLastItem) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(2.dp)
                                                                .weight(1f)
                                                                .background(Color(android.graphics.Color.parseColor(currentDayColorHex)))
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
                                                        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                                                        shadowElevation = 2.dp,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC BORDER
                                                        modifier = Modifier.fillMaxWidth().combinedClickable(
                                                            onClick = { viewModel.fetchPlaceDetails(place, trip?.destination ?: "") },
                                                            onLongClick = { placeMenuTarget = place }
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(16.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        text = place.name,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 16.sp,
                                                                        color = if (place.isVisited) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f, fill = false),
                                                                        textDecoration = if (place.isVisited) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                                    )
                                                                    if (place.isVisited) {
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Icon(
                                                                            imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                                                                            contentDescription = "Visited",
                                                                            tint = MaterialTheme.colorScheme.primary, // DYNAMIC ICON
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                                if (place.startTime.isNotEmpty() && place.endTime.isNotEmpty()) {
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp)) // DYNAMIC ICON
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("${place.startTime} - ${place.endTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) // DYNAMIC TEXT
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC BACKGROUND
                                                                ) {
                                                                    Text(
                                                                        text = place.category,
                                                                        fontSize = 12.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            // Thumbnail
                                                            LaunchedEffect(place.id) {
                                                                if (trip != null) {
                                                                    viewModel.fetchPhotoForPlace(place, trip!!.destination)
                                                                }
                                                            }
                                                            val photoUrl = placePhotos[place.id] ?: "https://picsum.photos/seed/${place.name.hashCode()}/200"
                                                            Image(
                                                                painter = rememberAsyncImagePainter(photoUrl),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .size(64.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant) // DYNAMIC BACKGROUND
                                                            )
                                                        }
                                                    }

                                                    if (!isLastItem) {
                                                        // Transit Row
                                                        val legKey = "${place.id}_${places[index + 1].id}"
                                                        val leg = routeLegs[legKey]

                                                        val isDriving = leg != null && leg.walkDuration > 900

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(vertical = 16.dp)
                                                        ) {
                                                            if (isDriving) {
                                                                Icon(Icons.Filled.DirectionsCar, contentDescription = "Drive", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) // DYNAMIC ICON
                                                            } else {
                                                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Walk", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) // DYNAMIC ICON
                                                            }

                                                            val text = if (leg != null) {
                                                                val durationToUse = if (isDriving) leg.driveDuration else leg.walkDuration
                                                                val distanceToUse = if (isDriving) leg.driveDistance else leg.walkDistance

                                                                val mins = (durationToUse / 60).toInt()
                                                                val km = String.format(java.util.Locale.US, "%.1f", distanceToUse / 1000.0)
                                                                val miles = String.format(java.util.Locale.US, "%.1f", distanceToUse * 0.000621371)
                                                                "$mins mins • $km km / $miles mi"
                                                            } else {
                                                                "Calculating..."
                                                            }
                                                            Text("$text ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)) // DYNAMIC TEXT

                                                            val localContext = androidx.compose.ui.platform.LocalContext.current
                                                            Text(
                                                                "Directions >",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary, // DYNAMIC TEXT
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.clickable {
                                                                    val originLat = place.lat
                                                                    val originLng = place.lng
                                                                    val destLat = places[index + 1].lat
                                                                    val destLng = places[index + 1].lng
                                                                    val mode = if (isDriving) "driving" else "walking"
                                                                    val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$originLat,$originLng&destination=$destLat,$destLng&travelmode=$mode")
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                                    localContext.startActivity(intent)
                                                                }.padding(4.dp)
                                                            )
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
                    },
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
                            ) {
                                MapEffect(places, currentDayColorHex) { mapView ->
                                    mapView.mapboxMap.loadStyle(com.mapbox.maps.Style.MAPBOX_STREETS) { style ->
                                        mapView.location.updateSettings {
                                            enabled = true
                                            pulsingEnabled = true
                                            locationPuck = com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck(withBearing = true)
                                        }

                                        val annotationApi = mapView.annotations
                                        val polylineManager = annotationApi.createPolylineAnnotationManager()
                                        val pointManager = annotationApi.createPointAnnotationManager()

                                        val routePoints = places.map { Point.fromLngLat(it.lng, it.lat) }
                                        if (routePoints.size > 1) {
                                            val polylineOptions = com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions()
                                                .withPoints(routePoints)
                                                .withLineColor(currentDayColorHex)
                                                .withLineWidth(4.0)
                                                .withLineJoin(com.mapbox.maps.extension.style.layers.properties.generated.LineJoin.ROUND)
                                            polylineManager.create(polylineOptions)

                                            // Fetch real directions route in the background
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                val route = com.example.pocketplanner.utils.RouteFetcher.getRoute(
                                                    routePoints,
                                                    com.example.pocketplanner.BuildConfig.MAPBOX_ACCESS_TOKEN
                                                )
                                                if (route != null) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        polylineManager.deleteAll()
                                                        val newOptions = com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions()
                                                            .withPoints(route)
                                                            .withLineColor(currentDayColorHex)
                                                            .withLineWidth(4.0)
                                                            .withLineJoin(com.mapbox.maps.extension.style.layers.properties.generated.LineJoin.ROUND)
                                                        polylineManager.create(newOptions)
                                                    }
                                                }
                                            }
                                        }

                                        val pointOptionsList = places.mapIndexed { index, place ->
                                            val bitmap = createNumberedMarkerBitmap(index + 1, currentDayColorHex)
                                            val imageId = "marker_${currentDayColorHex}_$index"
                                            style.addImage(imageId, bitmap)

                                            com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions()
                                                .withPoint(Point.fromLngLat(place.lng, place.lat))
                                                .withIconImage(imageId)
                                        }
                                        pointManager.create(pointOptionsList)
                                    }
                                }
                            }

                            // Top controls overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(40.dp).clickable(onClick = onNavigateBack)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                                }
                            }
                        }
                    }
                )

                // Custom Pill Bottom Navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavPill("Plan", true, Modifier.weight(1f)) { }
                            BottomNavPill("Expense", false, Modifier.weight(1f)) { onExpenseClick() }
                        }
                    }
                }

                // Add Calendar Bottom Sheet
                if (trip != null && showCalendarSheet) {
                    TripDatesCalendar(
                        tripStartDate = trip!!.startDate,
                        tripEndDate = trip!!.endDate,
                        onDismiss = { showCalendarSheet = false }
                    )
                }


                // Place Options Bottom Sheet
                if (placeMenuTarget != null) {
                    var showDayPicker by remember { mutableStateOf(false) }

                    ModalBottomSheet(
                        onDismissRequest = { placeMenuTarget = null; showDayPicker = false },
                        containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        windowInsets = WindowInsets(0),
                        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        val place = placeMenuTarget!!
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(vertical = 16.dp)
                        ) {
                            if (!showDayPicker) {
                                Text(
                                    text = place.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp)) // DYNAMIC DIVIDER

                                // Mark as Visited Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.toggleVisited(place)
                                            placeMenuTarget = null
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (place.isVisited) Icons.Filled.Close else Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = if (place.isVisited) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, // DYNAMIC ICON
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(if (place.isVisited) "Mark as Unvisited" else "Mark as Visited", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                                }

                                // Rearrange List
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            placeMenuTarget = null
                                            showRearrangeSheet = true
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Rearrange", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                                }

                                // Move to another day
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showDayPicker = true
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Move to another day", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                                }

                                // Remove
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.deletePlace(place)
                                            placeMenuTarget = null
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Remove Place", fontSize = 16.sp, color = MaterialTheme.colorScheme.error) // DYNAMIC TEXT
                                }
                            } else {
                                // Day Picker View
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = MaterialTheme.colorScheme.onSurface, contentDescription = "Back", modifier = Modifier.align(Alignment.CenterStart).clickable { showDayPicker = false }.padding(8.dp)) // DYNAMIC ICON
                                    Text("Select Day", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center)) // DYNAMIC TEXT
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp)) // DYNAMIC DIVIDER

                                val durationDays = if (trip != null) ((trip!!.endDate - trip!!.startDate) / 86400000).toInt() + 1 else 1
                                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                                    items(durationDays) { index ->
                                        val targetDay = index + 1
                                        val isCurrentDay = targetDay == place.dayNumber

                                        val dateStr = if (trip != null) {
                                            val instant = Instant.ofEpochMilli(trip!!.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                            val targetDate = instant.plusDays((targetDay - 1).toLong())
                                            val formatter = DateTimeFormatter.ofPattern("MMM d")
                                            targetDate.format(formatter)
                                        } else ""

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isCurrentDay) {
                                                    viewModel.movePlaceDay(place, targetDay)
                                                    placeMenuTarget = null
                                                    showDayPicker = false
                                                }
                                                .padding(horizontal = 24.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Day $targetDay - $dateStr",
                                                fontSize = 16.sp,
                                                color = if (isCurrentDay) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                                fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isCurrentDay) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("(Current)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) // DYNAMIC TEXT
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Rearrange Bottom Sheet
                if (showRearrangeSheet) {
                    var rearrangeTab by remember { mutableStateOf("Places") }
                    val durationDays = if (trip != null) ((trip!!.endDate - trip!!.startDate) / 86400000).toInt() + 1 else 1

                    ModalBottomSheet(
                        onDismissRequest = { showRearrangeSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        windowInsets = WindowInsets(0),
                        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.9f)
                        ) {
                            // Header
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text("Rearrange", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center)) // DYNAMIC TEXT
                                Text("Done", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterEnd).clickable { showRearrangeSheet = false }) // DYNAMIC TEXT
                            }

                            // Segmented Button Mock
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC BACKGROUND
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Days Tab
                                    Surface(
                                        modifier = Modifier.weight(1f).padding(4.dp).clickable { rearrangeTab = "Days" },
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (rearrangeTab == "Days") MaterialTheme.colorScheme.surface else Color.Transparent, // DYNAMIC BACKGROUND
                                        shadowElevation = if (rearrangeTab == "Days") 1.dp else 0.dp
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                            Text("Days", color = if (rearrangeTab == "Days") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) // DYNAMIC TEXT
                                        }
                                    }

                                    // Places Tab
                                    Surface(
                                        modifier = Modifier.weight(1f).padding(4.dp).clickable { rearrangeTab = "Places" },
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (rearrangeTab == "Places") MaterialTheme.colorScheme.surface else Color.Transparent, // DYNAMIC BACKGROUND
                                        shadowElevation = if (rearrangeTab == "Places") 1.dp else 0.dp
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                            Text("Places", color = if (rearrangeTab == "Places") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) // DYNAMIC TEXT
                                        }
                                    }
                                }
                            }

                            if (rearrangeTab == "Places") {
                                // Date Title
                                val dateStr = if (trip != null) {
                                    val instant = Instant.ofEpochMilli(trip!!.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                    val targetDate = instant.plusDays((selectedDay - 1).toLong())
                                    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
                                    val dayStr = targetDate.format(formatter)
                                    val dayNum = targetDate.dayOfMonth
                                    val suffix = when (dayNum % 10) {
                                        1 -> if (dayNum == 11) "th" else "st"
                                        2 -> if (dayNum == 12) "th" else "nd"
                                        3 -> if (dayNum == 13) "th" else "rd"
                                        else -> "th"
                                    }
                                    "$dayStr$suffix"
                                } else "Saturday, August 1st"

                                Text(
                                    text = dateStr,
                                    color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(places) { index, place ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC BACKGROUND
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(place.name, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) // DYNAMIC TEXT

                                                // Functional Up/Down arrows + Visual Drag handle
                                                if (index > 0) {
                                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { viewModel.swapPlaceOrder(place, places[index - 1]) }.padding(2.dp)) // DYNAMIC ICON
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                if (index < places.size - 1) {
                                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { viewModel.swapPlaceOrder(place, places[index + 1]) }.padding(2.dp)) // DYNAMIC ICON
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (rearrangeTab == "Days") {
                                Text(
                                    text = "All Days",
                                    color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val daysList = (1..durationDays).toList()
                                    itemsIndexed(daysList) { index, dayNum ->
                                        val dateStr = if (trip != null) {
                                            val instant = Instant.ofEpochMilli(trip!!.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                            val targetDate = instant.plusDays((dayNum - 1).toLong())
                                            val formatter = DateTimeFormatter.ofPattern("MMM d")
                                            targetDate.format(formatter)
                                        } else "Aug $dayNum"

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC BACKGROUND
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val placesForThisDay = allPlaces.filter { it.dayNumber == dayNum }
                                                val placesSummary = if (placesForThisDay.isEmpty()) "Free day" else placesForThisDay.joinToString(", ") { it.name }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Day $dayNum - $dateStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = if (dayNum == selectedDay) FontWeight.Bold else FontWeight.Normal) // DYNAMIC TEXT
                                                    Text(placesSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis) // DYNAMIC TEXT
                                                }

                                                if (index > 0) {
                                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { viewModel.swapDays(tripId, dayNum, daysList[index - 1]) }.padding(2.dp)) // DYNAMIC ICON
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                if (index < daysList.size - 1) {
                                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { viewModel.swapDays(tripId, dayNum, daysList[index + 1]) }.padding(2.dp)) // DYNAMIC ICON
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Place Details Bottom Sheet
                if (isFetchingPlaceDetails || selectedPlaceDetails != null) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.clearSelectedPlaceDetails() },
                        containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                        windowInsets = WindowInsets(0),
                        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        modifier = Modifier.fillMaxHeight(0.9f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            if (isFetchingPlaceDetails) {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // DYNAMIC COLOR
                                }
                            } else {
                                selectedPlaceDetails?.let { details ->
                                    val urls = details.photoUrls?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC ICON
                                            modifier = Modifier.clickable { viewModel.clearSelectedPlaceDetails() }
                                        )
                                        Spacer(modifier = Modifier.width(32.dp))
                                        val placeName = places.find { it.id == details.placeId }?.name ?: "Place Details"
                                        Text(
                                            text = placeName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary, // DYNAMIC TEXT
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(32.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC ICON
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Photo Carousel
                                    if (urls.isNotEmpty()) {
                                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { urls.size })
                                        androidx.compose.foundation.pager.HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxWidth().height(240.dp),
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            pageSpacing = if (urls.size > 1) 12.dp else 0.dp
                                        ) { page ->
                                            Image(
                                                painter = rememberAsyncImagePainter(urls[page]),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant) // DYNAMIC BACKGROUND
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Dots Indicator
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            repeat(urls.size) { iteration ->
                                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant // DYNAMIC COLORS
                                                Box(
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .size(6.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                                        // Info Pills (Hours & Price)
                                        if (details.formattedHours != null || details.price != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                                                shape = RoundedCornerShape(16.dp),
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    if (details.formattedHours != null) {
                                                        Column(modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp).weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { // DYNAMIC BACKGROUND
                                                                Icon(Icons.Filled.Schedule, contentDescription = "Hours", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                                                            }
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            Text("Hours", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                                                            Text(
                                                                text = details.formattedHours,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )
                                                        }

                                                        // Divider
                                                        if (details.price != null) {
                                                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant)) // DYNAMIC DIVIDER
                                                        }
                                                    }
                                                    if (details.price != null) {
                                                        Column(modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp).weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { // DYNAMIC BACKGROUND
                                                                Icon(Icons.Filled.Payments, contentDescription = "Price", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
                                                            }
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            Text("Price", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                                                            Text(
                                                                text = details.price,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))

                                        // About
                                        details.aiDescription?.let { desc ->
                                            Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) // DYNAMIC TEXT
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp) // DYNAMIC TEXT
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }

                                        // Local Tip
                                        details.aiTip?.let { tip ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer, // DYNAMIC BACKGROUND
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC BORDER
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box {
                                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                                        Icon(Icons.Filled.Lightbulb, contentDescription = "Tip", tint = MaterialTheme.colorScheme.onSecondaryContainer) // DYNAMIC ICON
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Local Tip", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) // DYNAMIC TEXT
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), lineHeight = 18.sp) // DYNAMIC TEXT
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }

                                        // Map
                                        val mapPlace = places.find { it.id == details.placeId }
                                        val placeNameForMap = mapPlace?.name ?: "Unknown Place"
                                        val displayAddress = details.address ?: "$placeNameForMap, ${trip?.destination ?: ""}".trimEnd(',', ' ')

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                if (mapPlace != null) {
                                                    val lat = mapPlace.lat
                                                    val lng = mapPlace.lng
                                                    val placeIndex = places.indexOf(mapPlace)
                                                    val markerLabel = if (placeIndex != -1 && placeIndex < 9) "${placeIndex + 1}" else "marker"
                                                    val pinColor = currentDayColorHex.removePrefix("#")
                                                    val mapUrl = "https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/pin-l-$markerLabel+$pinColor(${lng},${lat})/${lng},${lat},15,0/600x300@2x?access_token=${com.example.pocketplanner.BuildConfig.MAPBOX_ACCESS_TOKEN}"

                                                    Image(
                                                        painter = rememberAsyncImagePainter(mapUrl),
                                                        contentDescription = "Map",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxWidth().height(150.dp)
                                                    )
                                                } else {
                                                    Image(
                                                        painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1524661135-423995f22d0b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"), // Vintage map placeholder
                                                        contentDescription = "Map Placeholder",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxWidth().height(120.dp)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) // DYNAMIC ICON
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(displayAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis) // DYNAMIC TEXT
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    val context = androidx.compose.ui.platform.LocalContext.current
                                                    Button(
                                                        onClick = {
                                                            val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(displayAddress)}")
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                            context.startActivity(intent)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // DYNAMIC BUTTON
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) // DYNAMIC TEXT
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                    // End Details
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDatesCalendar(
    tripStartDate: Long,
    tripEndDate: Long,
    onDismiss: () -> Unit
) {
    val startLocalDate = Instant.ofEpochMilli(tripStartDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val endLocalDate = Instant.ofEpochMilli(tripEndDate).atZone(ZoneId.systemDefault()).toLocalDate()

    var currentMonth by remember { mutableStateOf(YearMonth.from(startLocalDate)) }

    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val rangeFormatter = DateTimeFormatter.ofPattern("MMM d")
    val rangeString = "${startLocalDate.format(rangeFormatter)} - ${endLocalDate.format(rangeFormatter)}"

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets = WindowInsets(0)
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
            // Header: Title and Done
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Trip dates", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                Text("Done", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterEnd).clickable { onDismiss() }) // DYNAMIC TEXT
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Month Switcher
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        modifier = Modifier.size(24.dp).clickable { currentMonth = currentMonth.minusMonths(1) },
                        tint = MaterialTheme.colorScheme.primary // DYNAMIC ICON
                    )
                    Text(currentMonth.format(formatter), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        modifier = Modifier.size(24.dp).clickable { currentMonth = currentMonth.plusMonths(1) },
                        tint = MaterialTheme.colorScheme.primary // DYNAMIC ICON
                    )
                }

                val isTripInCurrentMonth = (startLocalDate.year == currentMonth.year && startLocalDate.month == currentMonth.month) ||
                        (endLocalDate.year == currentMonth.year && endLocalDate.month == currentMonth.month)
                val rangeColor = if (isTripInCurrentMonth) MaterialTheme.colorScheme.onSurface else Color.Transparent // DYNAMIC TEXT
                Text(rangeString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = rangeColor)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Days of week
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                daysOfWeek.forEach { day ->
                    Text(day, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) // DYNAMIC TEXT
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()

            val firstDayOfWeekInt = firstDayOfMonth.dayOfWeek.value % 7

            val daysInGrid = mutableListOf<LocalDate>()

            val prevMonth = currentMonth.minusMonths(1)
            val prevMonthLastDay = prevMonth.atEndOfMonth().dayOfMonth
            for (i in firstDayOfWeekInt downTo 1) {
                daysInGrid.add(prevMonth.atDay(prevMonthLastDay - i + 1))
            }

            for (i in 1..lastDayOfMonth.dayOfMonth) {
                daysInGrid.add(currentMonth.atDay(i))
            }

            val nextMonth = currentMonth.plusMonths(1)
            var nextMonthDay = 1
            while (daysInGrid.size < 42) {
                daysInGrid.add(nextMonth.atDay(nextMonthDay++))
            }

            // Render rows
            Column(modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until daysInGrid.size / 7) {
                    Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        for (col in 0..6) {
                            val date = daysInGrid[row * 7 + col]
                            val isCurrentMonth = date.month == currentMonth.month

                            if (isCurrentMonth) {
                                val isStartDay = (date == startLocalDate)
                                val isEndDay = (date == endLocalDate)
                                val isWithinRange = !date.isBefore(startLocalDate) && !date.isAfter(endLocalDate)

                                val roundStart = isStartDay || col == 0
                                val roundEnd = isEndDay || col == 6

                                val backgroundColor = if (isWithinRange) MaterialTheme.colorScheme.primary else Color.Transparent // DYNAMIC BACKGROUND
                                val textColor = if (isWithinRange) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT

                                val shape = RoundedCornerShape(
                                    topStart = if (roundStart) 50.dp else 0.dp,
                                    bottomStart = if (roundStart) 50.dp else 0.dp,
                                    topEnd = if (roundEnd) 50.dp else 0.dp,
                                    bottomEnd = if (roundEnd) 50.dp else 0.dp
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp)
                                        .background(backgroundColor, shape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(date.dayOfMonth.toString(), color = textColor, fontSize = 16.sp)
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BottomNavPill(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, // DYNAMIC BACKGROUND
        shape = RoundedCornerShape(30.dp),
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                fontSize = 14.sp
            )
        }
    }
}

fun createNumberedMarkerBitmap(number: Int, colorHex: String, isMiniMap: Boolean = false): android.graphics.Bitmap {
    val size = if (isMiniMap) 91 else 90
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val center = size / 2f
    val radius = (size / 2f) - 8f

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor(colorHex)
        style = android.graphics.Paint.Style.FILL
        setShadowLayer(6f, 0f, 4f, android.graphics.Color.parseColor("#40000000"))
    }

    canvas.drawCircle(center, center, radius, paint)

    paint.clearShadowLayer()
    paint.apply {
        color = android.graphics.Color.WHITE // Keeping Native Graphics Colors for Mapbox compatibility
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(center, center, radius, paint)

    paint.apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
        textSize = 40f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val yPos = center - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(number.toString(), center, yPos, paint)

    return bitmap
}