package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.example.pocketplanner.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import java.util.Calendar
import androidx.compose.ui.graphics.Color
import com.example.pocketplanner.data.local.entity.TripEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripDetailsScreen(
    destinations: String,
    isGenerating: Boolean,
    coverPhotoUrl: String? = null,
    existingTrips: List<TripEntity> = emptyList(),
    exchangeRates: Map<String, Double> = emptyMap(),
    onNavigateBack: () -> Unit,
    onCreateTrip: (days: Int, name: String, startMillis: Long, endMillis: Long, customPhotoUrl: String?, budgetAmt: Double?, budgetCurr: String?) -> Unit
) {
    var tripName by remember { mutableStateOf("") }
    var isTrackerEnabled by remember { mutableStateOf(true) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState()
    val startTimePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    
    val endDatePickerState = rememberDatePickerState()
    val endTimePickerState = rememberTimePickerState(initialHour = 17, initialMinute = 0)
    
    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) {
                customImageUri = uri
            }
        }
    )
    
    var budgetAmount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var trackingMode by remember { mutableStateOf("High Accuracy") }
    val currencies = listOf("USD", "EUR", "GBP", "JPY", "AUD", "SGD", "VND")
    
    val dateTimeFormatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    fun getCombinedMillis(dateMillis: Long?, hour: Int, minute: Int): Long? {
        if (dateMillis == null) return null
        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateMillis
        }
        val localCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCalendar.timeInMillis
    }

    val startMillis = getCombinedMillis(startDatePickerState.selectedDateMillis, startTimePickerState.hour, startTimePickerState.minute)
    val endMillis = getCombinedMillis(endDatePickerState.selectedDateMillis, endTimePickerState.hour, endTimePickerState.minute)

    val startDateText = startMillis?.let { dateTimeFormatter.format(Date(it)) } ?: "Set date & time"
    val endDateText = endMillis?.let { dateTimeFormatter.format(Date(it)) } ?: "Optional"

    val overlappingTripName = remember(startMillis, endMillis, existingTrips) {
        if (startMillis == null || endMillis == null) return@remember null
        existingTrips.find { trip ->
            startMillis <= trip.endDate && endMillis >= trip.startDate
        }?.name
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "New trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001F3F) // Dark Navy
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFEEEEEE), CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF005b9f), // Theme Blue
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(24.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = {
                            if (startMillis != null) {
                                val finalEndMillis = endMillis ?: startMillis
                                val days = ((finalEndMillis - startMillis) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                                val photoUrlToPass = if (customImageUri != null) customImageUri.toString() else coverPhotoUrl
                                val bAmt = budgetAmount.toDoubleOrNull()
                                val bCurr = if (bAmt != null) selectedCurrency else null
                                val finalTripName = if (tripName.isNotBlank()) tripName else "Trip to $destinations"
                                onCreateTrip(days, finalTripName, startMillis, finalEndMillis, photoUrlToPass, bAmt, bCurr)
                            }
                        },
                        enabled = startMillis != null && !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b9f)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Generating Trip...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Create Trip with AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(painter = painterResource(id = R.drawable.ai), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val scrollState = androidx.compose.foundation.rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                // Cover Image Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    val imageUrl = customImageUri ?: coverPhotoUrl ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Change Cover Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Change Cover",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                // The white "Bottom Sheet" area
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        OutlinedTextField(
                            value = tripName,
                            onValueChange = { if (it.length <= 50) tripName = it },
                            placeholder = { 
                                Text(
                                    "Name your trip", 
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.pocketplanner.R.drawable.ntpen),
                                    contentDescription = null,
                                    tint = Color.Unspecified, // Assuming it's a colored icon or we can tint it later if needed. If it's a monochrome icon, tinting it DarkGray is standard, but Unspecified keeps original colors.
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingIcon = {
                                Text(
                                    text = "${tripName.length}/50",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                focusedBorderColor = Color(0xFF005b9f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Date Selection Sub-Card matching new mockup
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Top half: Icon and "Trip dates"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.pocketplanner.R.drawable.ntcalendar),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Trip dates",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF001F3F)
                                    )
                                }
                                
                                HorizontalDivider(color = Color(0xFFE0E0E0))
                                
                                // Bottom half: Dates and Arrow
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start Date
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showStartDatePicker = true }
                                    ) {
                                        Text("Start date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF001F3F))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            startDateText, 
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                            color = if (startDateText == "Set date & time") Color(0xFF0091EA) else Color(0xFF001F3F),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    
                                    // Arrow sitting over the dashed line
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "To", tint = Color(0xFF001F3F), modifier = Modifier.size(20.dp))

                                    // End Date
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showEndDatePicker = true }
                                    ) {
                                        Text("End date", style = MaterialTheme.typography.labelMedium, color = Color(0xFF001F3F))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            endDateText, 
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                            color = if (endDateText == "Optional") Color(0xFF0091EA) else Color(0xFF001F3F),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (overlappingTripName != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFFFF3E0), // Light orange background
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFF57C00), // Dark orange
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Warning: This trip overlaps with your existing trip \"$overlappingTripName\".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE65100), // Darker orange
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Budget Box
                        OutlinedTextField(
                            value = budgetAmount,
                            onValueChange = { budgetAmount = it },
                            placeholder = { 
                                Text(
                                    "Budget (Optional)", 
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            leadingIcon = {
                                Icon(painter = painterResource(id = R.drawable.ntbudget), contentDescription = "Budget", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                            },
                            trailingIcon = {
                                Box {
                                    TextButton(onClick = { currencyDropdownExpanded = true }) {
                                        Text(selectedCurrency, color = Color(0xFF001F3F), fontWeight = FontWeight.Bold)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Currency", tint = Color(0xFF001F3F))
                                    }
                                    DropdownMenu(
                                        expanded = currencyDropdownExpanded,
                                        onDismissRequest = { currencyDropdownExpanded = false }
                                    ) {
                                        currencies.forEach { curr ->
                                            DropdownMenuItem(
                                                text = { Text(curr) },
                                                onClick = {
                                                    selectedCurrency = curr
                                                    currencyDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF0091EA),
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Live VND Conversion
                        if (budgetAmount.isNotEmpty() && exchangeRates.isNotEmpty()) {
                            val vndRate = exchangeRates["VND"] ?: 25000.0
                            val selectedRate = exchangeRates[selectedCurrency] ?: 1.0
                            val parsedAmount = budgetAmount.toDoubleOrNull() ?: 0.0
                            val vndAmount = parsedAmount * (vndRate / selectedRate)
                            
                            val formattedVnd = try {
                                val format = java.text.NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                                format.format(vndAmount)
                            } catch (e: Exception) {
                                "%,.0f VND".format(Locale.US, vndAmount)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = formattedVnd,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                leadingIcon = {
                                    Text("≈", color = Color.Gray, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
                                },
                                trailingIcon = {
                                    Text("VND", color = Color(0xFF001F3F), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color.LightGray,
                                    unfocusedBorderColor = Color.LightGray,
                                    disabledTextColor = Color.DarkGray,
                                    disabledContainerColor = Color.White,
                                    disabledBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Travel Tracker Section Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ntlocation), contentDescription = "Tracker", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Travel Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF001F3F), modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = isTrackerEnabled,
                                        onCheckedChange = { isTrackerEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0091EA))
                                    )
                                }

                                if (isTrackerEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // High Accuracy Option (was Balanced)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (trackingMode == "High Accuracy") Color(0xFFE3F2FD) else Color.White,
                                        border = if (trackingMode == "High Accuracy") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0091EA)) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                        modifier = Modifier.fillMaxWidth().clickable { trackingMode = "High Accuracy" }
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                            RadioButton(
                                                selected = trackingMode == "High Accuracy",
                                                onClick = { trackingMode = "High Accuracy" },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0091EA))
                                            )
                                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                                Text("High Accuracy", fontWeight = FontWeight.Bold, color = Color(0xFF001F3F))
                                                Text("Neighborhood-level tracking. Background updates when moving significantly.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(painter = painterResource(id = R.drawable.ntrange), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                                    Text(" ~ 100m precision", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp, end = 12.dp), maxLines = 1)
                                                    Icon(painter = painterResource(id = R.drawable.ntenergy), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                                    Text(" Medium battery", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Balanced Option (was Low Power Mode)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (trackingMode == "Balanced") Color(0xFFE3F2FD) else Color.White,
                                        border = if (trackingMode == "Balanced") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0091EA)) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                        modifier = Modifier.fillMaxWidth().clickable { trackingMode = "Balanced" }
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                            RadioButton(
                                                selected = trackingMode == "Balanced",
                                                onClick = { trackingMode = "Balanced" },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0091EA))
                                            )
                                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                                Text("Balanced", fontWeight = FontWeight.Bold, color = Color(0xFF001F3F))
                                                Text("City-level tracking. Updates only occasionally to maximize battery savings.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(painter = painterResource(id = R.drawable.ntrange), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                                    Text(" ~ 10km precision", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp, end = 12.dp), maxLines = 1)
                                                    Icon(painter = painterResource(id = R.drawable.ntenergy), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                                    Text(" Minimal battery", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Extra padding at the bottom for scrolling past floating button
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }


    // Start Date Picker Dialog
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showStartDatePicker = false 
                        showStartTimePicker = true
                    },
                    enabled = startDatePickerState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                title = { Text(text = "Select Start Date", modifier = Modifier.padding(start = 24.dp, top = 24.dp)) },
                showModeToggle = false
            )
        }
    }

    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Start Time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = startTimePickerState)
                }
            }
        )
    }

    // End Date Picker Dialog
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showEndDatePicker = false 
                        showEndTimePicker = true
                    },
                    enabled = endDatePickerState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                title = { Text(text = "Select End Date", modifier = Modifier.padding(start = 24.dp, top = 24.dp)) },
                showModeToggle = false
            )
        }
    }

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select End Time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = endTimePickerState)
                }
            }
        )
    }
}
