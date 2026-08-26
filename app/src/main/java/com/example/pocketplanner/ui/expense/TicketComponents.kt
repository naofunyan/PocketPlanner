package com.example.pocketplanner.ui.expense

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import com.yalantis.ucrop.UCrop
import android.app.Activity.RESULT_OK
import android.view.WindowManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

// ============================================================
//  1. TICKET TAB CONTENT
// ============================================================

@Composable
fun TicketTabContent(
    tickets: List<TicketEntity>,
    allTrips: List<TripEntity>,
    selectedFilter: TicketFilter,
    onFilterSelected: (TicketFilter) -> Unit,
    onTicketClick: (TicketEntity) -> Unit,
    onDeleteTicket: (TicketEntity) -> Unit,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text(stringResource(R.string.ticket_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }, // DYNAMIC
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.ticket_search_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) }, // DYNAMIC
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ticket_clear_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, // DYNAMIC
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                unfocusedContainerColor = MaterialTheme.colorScheme.surface // DYNAMIC
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
        )

        // --- Filter Chips ---
        TicketFilterChips(
            tickets = tickets,
            allTrips = allTrips,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Ticket List or Empty State ---
        if (tickets.isEmpty()) {
            EmptyTicketState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tickets, key = { it.id }) { ticket ->
                    val tripName = ticket.tripId?.let { tripId ->
                        allTrips.find { it.id == tripId }
                            ?.let { if (it.name.isNotBlank()) it.name else stringResource(R.string.expense_trip_to, it.destination) }
                    }
                    TicketCard(
                        ticket = ticket,
                        tripName = tripName,
                        onClick = { onTicketClick(ticket) },
                        onDelete = { onDeleteTicket(ticket) }
                    )
                }
            }
        }
    }
}

// ============================================================
//  2. FILTER CHIPS
// ============================================================

@Composable
private fun TicketFilterChips(
    tickets: List<TicketEntity>,
    allTrips: List<TripEntity>,
    selectedFilter: TicketFilter,
    onFilterSelected: (TicketFilter) -> Unit
) {
    val filters = buildList<TicketFilter> {
        add(TicketFilter.All)
        add(TicketFilter.General)
        allTrips.forEach { trip ->
            add(TicketFilter.Trip(
                tripId = trip.id,
                tripName = if (trip.name.isNotBlank()) trip.name else stringResource(R.string.expense_trip_to, trip.destination)
            ))
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters.size) { index ->
            val filter = filters[index]
            val isSelected = when {
                filter is TicketFilter.All && selectedFilter is TicketFilter.All -> true
                filter is TicketFilter.General && selectedFilter is TicketFilter.General -> true
                filter is TicketFilter.Trip && selectedFilter is TicketFilter.Trip
                        && filter.tripId == selectedFilter.tripId -> true
                else -> false
            }

            val label = when (filter) {
                is TicketFilter.All -> stringResource(R.string.ticket_filter_all)
                is TicketFilter.General -> stringResource(R.string.ticket_filter_general)
                is TicketFilter.Trip -> filter.tripName
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary, // DYNAMIC
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary // DYNAMIC
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ============================================================
//  3. TICKET CARD
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketCard(
    ticket: TicketEntity,
    tripName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val typeIcon = when (ticket.type) {
        "Flight" -> Icons.Filled.Flight
        "Hotel" -> Icons.Filled.Hotel
        "Event" -> Icons.Filled.LocalActivity
        "Train" -> Icons.Filled.Train
        "Bus" -> Icons.Filled.DirectionsBus
        else -> Icons.Filled.ConfirmationNumber
    }

    // Keep Semantic Colors for icons, but adapt the cards
    val typeColor = when (ticket.type) {
        "Flight" -> Color(0xFF2196F3)
        "Hotel" -> Color(0xFFFF9800)
        "Event" -> Color(0xFFE91E63)
        "Train" -> Color(0xFF4CAF50)
        "Bus" -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, // DYNAMIC
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC
                modifier = Modifier.size(72.dp)
            ) {
                val thumbPath = ticket.thumbnailUri ?: ticket.imageUri
                AsyncImage(
                    model = File(thumbPath),
                    contentDescription = stringResource(R.string.ticket_image_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = ticket.type,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ticket.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface, // DYNAMIC
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${getTranslatedTicketType(ticket.type)} • ${dateFormatter.format(Date(ticket.dateTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC
                )

                Spacer(modifier = Modifier.height(4.dp))
                val status = TicketStatus.fromDateTime(ticket.dateTime)

                // Adaptive Semantic Colors for Badges
                val (statusText, statusColor, statusBgColor) = when (status) {
                    is TicketStatus.Today -> Triple(stringResource(R.string.ticket_status_today), Color.White, Color(0xFF2196F3))
                    is TicketStatus.Tomorrow -> Triple(stringResource(R.string.ticket_status_tomorrow), Color.White, Color(0xFFFF9800))
                    is TicketStatus.Upcoming -> Triple(stringResource(R.string.ticket_status_upcoming, status.daysUntil), MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondaryContainer) // DYNAMIC
                    is TicketStatus.Later -> Triple(status.displayDate, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant) // DYNAMIC
                    is TicketStatus.Expired -> Triple(stringResource(R.string.ticket_status_expired), MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer) // DYNAMIC
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBgColor
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (!ticket.confirmationCode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.ticket_conf_prefix, ticket.confirmationCode),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary // DYNAMIC
                    )
                }

                if (!ticket.qrContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.ticket_qr_prefix, ticket.qrContent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (tripName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer // DYNAMIC
                    ) {
                        Text(
                            text = tripName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer, // DYNAMIC
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.ticket_delete_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.ticket_delete_message, ticket.title), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.expense_delete_confirm), color = MaterialTheme.colorScheme.error) // DYNAMIC
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.expense_delete_cancel))
                }
            }
        )
    }
}

// ============================================================
//  4. EMPTY STATE
// ============================================================

@Composable
private fun EmptyTicketState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.ConfirmationNumber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.ticket_empty_title), color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
        Text(
            stringResource(R.string.ticket_empty_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // DYNAMIC
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )
    }
}

// ============================================================
//  5. ADD TICKET FORM (Bottom Sheet Content)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTicketForm(
    allTrips: List<TripEntity>,
    viewModel: TicketViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val isScanning by viewModel.isScanning.collectAsState()
    var hasScanned by remember { mutableStateOf(false) }
    var ocrRawText by remember { mutableStateOf<String?>(null) }
    var qrContent by remember { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            if (croppedUri != null) {
                imageUri = croppedUri
                hasScanned = false
            }
        }
    }

    fun launchCrop(sourceUri: Uri) {
        originalImageUri = sourceUri

        val destFile = File(context.cacheDir, "cropped_ticket_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val cropIntent = UCrop.of(sourceUri, destUri)
            .withAspectRatio(0f, 0f)
            .withOptions(UCrop.Options().apply {
                setToolbarTitle("Crop to Ticket")
                setToolbarColor(android.graphics.Color.parseColor("#FF005b9f"))
                setStatusBarColor(android.graphics.Color.parseColor("#FF004080"))
                setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF005b9f"))
                setFreeStyleCropEnabled(true)
                setShowCropGrid(true)
                setShowCropFrame(true)
            })
            .getIntent(context)

        cropLauncher.launch(cropIntent)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                launchCrop(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                launchCrop(tempCameraUri!!)
            }
        }
    )

    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Flight") }
    var confirmationCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val selectedMillis = remember(datePickerState.selectedDateMillis) {
        val selectedUtc = datePickerState.selectedDateMillis
        if (selectedUtc != null) {
            val now = Calendar.getInstance()
            val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedUtc
            }
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                set(Calendar.SECOND, now.get(Calendar.SECOND))
            }.timeInMillis
        } else {
            System.currentTimeMillis()
        }
    }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val displayDate = dateFormatter.format(Date(selectedMillis))

    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var showTripDropdown by remember { mutableStateOf(false) }
    val selectedTripName = selectedTripId?.let { id ->
        allTrips.find { it.id == id }
            ?.let { if (it.name.isNotBlank()) it.name else stringResource(R.string.expense_trip_to, it.destination) }
    } ?: stringResource(R.string.ticket_trip_none)

    data class TicketType(val name: String, val icon: ImageVector, val color: Color)

    val ticketTypes = listOf(
        TicketType("Flight", Icons.Filled.Flight, Color(0xFF2196F3)),
        TicketType("Hotel", Icons.Filled.Hotel, Color(0xFFFF9800)),
        TicketType("Event", Icons.Filled.LocalActivity, Color(0xFFE91E63)),
        TicketType("Train", Icons.Filled.Train, Color(0xFF4CAF50)),
        TicketType("Bus", Icons.Filled.DirectionsBus, Color(0xFF9C27B0)),
        TicketType("Other", Icons.Filled.ConfirmationNumber, Color(0xFF607D8B))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // DYNAMIC
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.expense_back_cd), tint = MaterialTheme.colorScheme.onBackground) // DYNAMIC
            }
            Text(
                stringResource(R.string.ticket_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, // DYNAMIC
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ========== IMAGE CAPTURE CARD ==========
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface, // DYNAMIC
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.ticket_image_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(16.dp))

                    if (imageUri == null) {
                        val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) }
                        val outlineColor = MaterialTheme.colorScheme.outline // Fix for Canvas draw behind
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = outlineColor, // DYNAMIC
                                        style = Stroke(width = 4f, pathEffect = dashPathEffect),
                                        cornerRadius = CornerRadius(16.dp.toPx())
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) { // DYNAMIC
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.padding(10.dp)) // DYNAMIC
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(stringResource(R.string.ticket_tap_capture), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                                Text(stringResource(R.string.ticket_qr_preserved), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) // DYNAMIC
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = stringResource(R.string.ticket_image_cd),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clickable {
                                        imageUri = null
                                        originalImageUri = null
                                        hasScanned = false
                                        ocrRawText = null
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.ticket_remove_cd),
                                    tint = Color.White,
                                    modifier = Modifier.padding(4.dp).size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!hasScanned) {
                            Button(
                                onClick = {
                                    imageUri?.let { uri ->
                                        scope.launch {
                                            val result = viewModel.runOcr(uri)
                                            ocrRawText = result.rawText

                                            if (result.suggestedTitle.isNotBlank() && title.isBlank()) {
                                                title = result.suggestedTitle
                                            }
                                            if (result.suggestedConfirmationCode.isNotBlank() && confirmationCode.isBlank()) {
                                                confirmationCode = result.suggestedConfirmationCode
                                            }
                                            if (result.suggestedType.isNotBlank()) {
                                                val validTypes = listOf("Flight", "Hotel", "Event", "Train", "Bus", "Other")
                                                if (result.suggestedType in validTypes) {
                                                    selectedType = result.suggestedType
                                                }
                                            }
                                            if (result.suggestedDate != null) {
                                                datePickerState.selectedDateMillis = result.suggestedDate
                                            }

                                            originalImageUri?.let { origUri ->
                                                qrContent = viewModel.scanBarcode(origUri)
                                            }

                                            hasScanned = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // DYNAMIC
                                enabled = !isScanning
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        // DYNAMIC
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ticket_scanning)) // DYNAMIC
                                } else {
                                    Icon(Icons.Filled.DocumentScanner, contentDescription = null) // DYNAMIC
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ticket_scan_ocr)) // DYNAMIC
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer // DYNAMIC
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp)) // DYNAMIC
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.ticket_scanned_success), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer) // DYNAMIC
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========== DETAILS CARD ==========
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface, // DYNAMIC
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // --- Title ---
                    Text(stringResource(R.string.ticket_title_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text(stringResource(R.string.ticket_title_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, // DYNAMIC
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Type selector ---
                    Text(stringResource(R.string.ticket_type_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ticketTypes.forEach { type ->
                            val isSelected = selectedType == type.name
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, // DYNAMIC
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant // DYNAMIC
                                ),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedType = type.name }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = getTranslatedTicketType(type.name),
                                        tint = type.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = getTranslatedTicketType(type.name),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, // DYNAMIC
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Date picker ---
                    val dateLabel = stringResource(R.string.ticket_date_label)
                    Text(dateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayDate,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = dateLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, // DYNAMIC
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showDatePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Confirmation code ---
                    Text(stringResource(R.string.ticket_conf_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmationCode,
                        onValueChange = { confirmationCode = it },
                        placeholder = { Text(stringResource(R.string.ticket_conf_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, // DYNAMIC
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Link to trip ---
                    Text(stringResource(R.string.ticket_link_trip_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = showTripDropdown,
                        onExpandedChange = { showTripDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTripName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTripDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = showTripDropdown,
                            onDismissRequest = { showTripDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ticket_trip_none)) },
                                onClick = {
                                    selectedTripId = null
                                    showTripDropdown = false
                                },
                                leadingIcon = {
                                    if (selectedTripId == null) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // DYNAMIC
                                    }
                                }
                            )
                            allTrips.forEach { trip ->
                                val name = if (trip.name.isNotBlank()) trip.name else stringResource(R.string.expense_trip_to, trip.destination)
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedTripId = trip.id
                                        showTripDropdown = false
                                    },
                                    leadingIcon = {
                                        if (selectedTripId == trip.id) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) // DYNAMIC
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Notes ---
                    Text(stringResource(R.string.ticket_notes_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text(stringResource(R.string.ticket_notes_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, // DYNAMIC
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ========== SAVE BUTTON ==========
            val untitledTicket = stringResource(R.string.ticket_untitled)
            Button(
                onClick = {
                    imageUri?.let { uri ->
                        viewModel.addTicket(
                            title = title.ifBlank { untitledTicket },
                            type = selectedType,
                            dateTime = selectedMillis,
                            imageUri = uri,
                            tripId = selectedTripId,
                            confirmationCode = confirmationCode.ifBlank { null },
                            notes = notes,
                            ocrRawText = ocrRawText,
                            qrContent = qrContent
                        )
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary // DYNAMIC
                ),
                enabled = imageUri != null && title.isNotBlank()
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null) // DYNAMIC
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ticket_save_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold) // DYNAMIC
            }
        }
    }

    // ---- DIALOGS ----

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.expense_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.expense_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(R.string.ticket_upload_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, // DYNAMIC
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.expense_choose_gallery), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC
                        }
                    }
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            val tempFile = File.createTempFile("ticket_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.expense_take_photo), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text(stringResource(R.string.expense_cancel)) }
            }
        )
    }
}

// ============================================================
//  6. TICKET IMAGE VIEWER (Full-screen with pinch-to-zoom)
// ============================================================

@Composable
fun TicketImageViewer(
    ticket: TicketEntity,
    viewModel: TicketViewModel,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f

        window?.attributes = window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }

        onDispose {
            window?.attributes = window?.attributes?.apply {
                screenBrightness = originalBrightness
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black) // Intentionally kept Black for image viewing
        ) {
            AsyncImage(
                model = File(ticket.imageUri),
                contentDescription = stringResource(R.string.ticket_full_view_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            if (scale > 1f) {
                                val maxOffsetX = (scale - 1f) * size.width / 2f
                                val maxOffsetY = (scale - 1f) * size.height / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .clickable(onClick = onDismiss)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.ticket_close_cd),
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }

            var showBackupQr by remember { mutableStateOf(false) }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = ticket.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White // Intentionally kept White for image viewing
                    )
                    if (!ticket.confirmationCode.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.ticket_viewer_conf, ticket.confirmationCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f) // Intentionally kept White
                        )
                    }

                    if (!ticket.qrContent.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (showBackupQr) {
                            val qrBitmap = remember(ticket.qrContent) {
                                viewModel.generateQrBitmap(ticket.qrContent!!)
                            }
                            if (qrBitmap != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = stringResource(R.string.ticket_backup_qr_cd),
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(8.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showBackupQr = false }) {
                                Text(stringResource(R.string.ticket_hide_qr), color = Color.White)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showBackupQr = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.ticket_show_qr))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getTranslatedTicketType(type: String): String {
    return when(type) {
        "Flight" -> stringResource(R.string.ticket_type_flight)
        "Hotel" -> stringResource(R.string.ticket_type_hotel)
        "Event" -> stringResource(R.string.ticket_type_event)
        "Train" -> stringResource(R.string.ticket_type_train)
        "Bus" -> stringResource(R.string.ticket_type_bus)
        "Other" -> stringResource(R.string.ticket_type_other)
        else -> type
    }
}