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
import androidx.compose.ui.geometry.Offset
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

// ============================================================
//  1. TICKET TAB CONTENT (replaces the placeholder)
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
            placeholder = { Text("Search tickets...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF005b9f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
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
                    bottom = 120.dp // Clear the FAB and bottom nav
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tickets, key = { it.id }) { ticket ->
                    val tripName = ticket.tripId?.let { tripId ->
                        allTrips.find { it.id == tripId }
                            ?.let { if (it.name.isNotBlank()) it.name else "Trip to ${it.destination}" }
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
    // Build the list of filter options
    // "All" is always first, "General" is second if there are unlinked tickets
    // Then one chip per trip that has at least one linked ticket
    val filters = buildList<TicketFilter> {
        add(TicketFilter.All)
        add(TicketFilter.General)
        // Find trips that have at least one ticket linked to them
        // (we check against the FULL unfiltered ticket list via allTrips linkage)
        allTrips.forEach { trip ->
            add(TicketFilter.Trip(
                tripId = trip.id,
                tripName = if (trip.name.isNotBlank()) trip.name else "Trip to ${trip.destination}"
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
                is TicketFilter.All -> "All"
                is TicketFilter.General -> "General"
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
                    selectedContainerColor = Color(0xFF005b9f),
                    selectedLabelColor = Color.White
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
        color = Color.White,
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
                color = Color(0xFFF0F4F8),
                modifier = Modifier.size(72.dp)
            ) {
                val thumbPath = ticket.thumbnailUri ?: ticket.imageUri
                AsyncImage(
                    model = File(thumbPath),
                    contentDescription = "Ticket image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Title row with type icon
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Type + date
                Text(
                    text = "${ticket.type} • ${dateFormatter.format(Date(ticket.dateTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Confirmation code (if present)
                if (!ticket.confirmationCode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Conf: ${ticket.confirmationCode}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF005b9f)
                    )
                }

                // QR content (if found)
                if (!ticket.qrContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "QR: ${ticket.qrContent}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF607D8B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Trip badge (if linked)
                if (tripName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = tripName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ticket", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${ticket.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Add your first ticket!", color = Color.Gray)
        Text(
            "Tap + to scan a boarding pass, event ticket, or hotel booking.",
            color = Color.Gray.copy(alpha = 0.7f),
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

    // --- Image state ---
    var imageUri by remember { mutableStateOf<Uri?>(null) }         // Final (cropped) image for OCR
    var originalImageUri by remember { mutableStateOf<Uri?>(null) }  // Original untouched image for storage
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // --- OCR state ---
    val isScanning by viewModel.isScanning.collectAsState()
    var hasScanned by remember { mutableStateOf(false) }
    var ocrRawText by remember { mutableStateOf<String?>(null) }
    var qrContent by remember { mutableStateOf<String?>(null) }

    // --- Crop launcher ---
    // This receives the result AFTER the user finishes cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            if (croppedUri != null) {
                imageUri = croppedUri  // Use cropped version for display + OCR
                hasScanned = false
            }
        }
    }

    // Helper function to launch UCrop
    fun launchCrop(sourceUri: Uri) {
        originalImageUri = sourceUri

        val destFile = File(context.cacheDir, "cropped_ticket_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val cropIntent = UCrop.of(sourceUri, destUri)
            .withAspectRatio(0f, 0f)  // ← ADD THIS: starts in free crop, no ratio lock
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

    // --- Gallery launcher → goes to crop ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                launchCrop(uri)
            }
        }
    )

    // --- Camera launcher → goes to crop ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                launchCrop(tempCameraUri!!)
            }
        }
    )

    // --- Form fields ---
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Flight") }
    var confirmationCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // --- Date state ---
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

    // --- Trip linking ---
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var showTripDropdown by remember { mutableStateOf(false) }
    val selectedTripName = selectedTripId?.let { id ->
        allTrips.find { it.id == id }
            ?.let { if (it.name.isNotBlank()) it.name else "Trip to ${it.destination}" }
    } ?: "None"

    // --- Ticket types ---
    data class TicketType(val name: String, val icon: ImageVector, val color: Color)

    val ticketTypes = listOf(
        TicketType("Flight", Icons.Filled.Flight, Color(0xFF2196F3)),
        TicketType("Hotel", Icons.Filled.Hotel, Color(0xFFFF9800)),
        TicketType("Event", Icons.Filled.LocalActivity, Color(0xFFE91E63)),
        TicketType("Train", Icons.Filled.Train, Color(0xFF4CAF50)),
        TicketType("Bus", Icons.Filled.DirectionsBus, Color(0xFF9C27B0)),
        TicketType("Other", Icons.Filled.ConfirmationNumber, Color(0xFF607D8B))
    )

    // ---- UI ----

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FA))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
            }
            Text(
                "Add Ticket",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
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
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TICKET IMAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (imageUri == null) {
                        // Dashed upload box (same pattern as AddExpenseForm)
                        val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color(0xFFB0BEC5),
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
                                Surface(shape = CircleShape, color = Color(0xFFE3F2FD), modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.padding(10.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Tap to capture or import ticket", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("QR codes will be preserved in original quality", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        // Show the selected image
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Ticket",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            // Remove button
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
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.padding(4.dp).size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- SCAN TICKET BUTTON ---
                        if (!hasScanned) {
                            Button(
                                onClick = {
                                    imageUri?.let { uri ->
                                        scope.launch {
                                            val result = viewModel.runOcr(uri)
                                            ocrRawText = result.rawText

                                            // Auto-fill fields with OCR results
                                            if (result.suggestedTitle.isNotBlank() && title.isBlank()) {
                                                title = result.suggestedTitle
                                            }
                                            if (result.suggestedConfirmationCode.isNotBlank() && confirmationCode.isBlank()) {
                                                confirmationCode = result.suggestedConfirmationCode
                                            }
                                            if (result.suggestedDate != null) {
                                                datePickerState.selectedDateMillis = result.suggestedDate
                                            }

                                            // Also scan for QR/barcode on the ORIGINAL image
                                            originalImageUri?.let { origUri ->
                                                qrContent = viewModel.scanBarcode(origUri)
                                            }

                                            hasScanned = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                enabled = !isScanning
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning...")
                                } else {
                                    Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Ticket (OCR)")
                                }
                            }
                        } else {
                            // Show success badge after scanning
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scanned! Fields auto-filled below.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
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
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // --- Title ---
                    Text("Title", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Vietnam Airlines VN123", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Type selector (chip row) ---
                    Text("Type", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                                color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF005b9f) else Color(0xFFE0E0E0)
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
                                        contentDescription = type.name,
                                        tint = type.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color(0xFF005b9f) else Color.DarkGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Date picker ---
                    Text("Date", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayDate,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Date", tint = Color.Gray) },
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
                    Text("Confirmation Code (Optional)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmationCode,
                        onValueChange = { confirmationCode = it },
                        placeholder = { Text("e.g. ABC123", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Link to trip (optional dropdown) ---
                    Text("Link to Trip (Optional)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                            // "None" option
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedTripId = null
                                    showTripDropdown = false
                                },
                                leadingIcon = {
                                    if (selectedTripId == null) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF005b9f))
                                    }
                                }
                            )
                            // Trip options
                            allTrips.forEach { trip ->
                                val name = if (trip.name.isNotBlank()) trip.name else "Trip to ${trip.destination}"
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedTripId = trip.id
                                        showTripDropdown = false
                                    },
                                    leadingIcon = {
                                        if (selectedTripId == trip.id) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF005b9f))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Notes ---
                    Text("Notes (Optional)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add any extra details...", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ========== SAVE BUTTON ==========
            Button(
                onClick = {
                    // Use originalImageUri for storage (preserves QR),
                    // OCR was run on the cropped version
                    imageUri?.let { uri ->
                        viewModel.addTicket(
                            title = title.ifBlank { "Untitled Ticket" },
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
                    containerColor = Color(0xFF005b9f),
                    disabledContainerColor = Color(0xFF005b9f).copy(alpha = 0.4f)
                ),
                enabled = imageUri != null && title.isNotBlank()
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ---- DIALOGS ----

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    // Image source dialog (Camera / Gallery)
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Add Ticket Image", fontWeight = FontWeight.Bold) },
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
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery", fontSize = 16.sp)
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
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Take a Photo", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) { Text("Cancel") }
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
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Auto-brightness: max brightness when viewing, restore on close
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f

        // Set to max brightness
        window?.attributes = window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL // = 1.0f
        }

        onDispose {
            // Restore original brightness when the viewer is closed
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
                .background(Color.Black)
        ) {
            // Zoomable image — uses the ORIGINAL imageUri for max quality
            AsyncImage(
                model = File(ticket.imageUri),
                contentDescription = "Ticket full view",
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
                                // Only allow panning when zoomed in
                                // Constrain so the image can't fly off screen
                                val maxOffsetX = (scale - 1f) * size.width / 2f
                                val maxOffsetY = (scale - 1f) * size.height / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                // Reset position when back to 1x
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )

            // Close button
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
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Ticket info + QR backup at the bottom
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
                        color = Color.White
                    )
                    if (!ticket.confirmationCode.isNullOrBlank()) {
                        Text(
                            text = "Confirmation: ${ticket.confirmationCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // QR backup section
                    if (!ticket.qrContent.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (showBackupQr) {
                            // Show the regenerated QR code
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
                                        contentDescription = "Backup QR Code",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(8.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showBackupQr = false }) {
                                Text("Hide QR", color = Color.White)
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
                                Text("Show Backup QR")
                            }
                        }
                    }
                }
            }
        }
    }
}