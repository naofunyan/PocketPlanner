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
import androidx.compose.material.icons.filled.Save
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
    editTripId: String? = null,
    existingTripData: TripEntity? = null,
    onNavigateBack: () -> Unit,
    onCreateTrip: (days: Int, name: String, startMillis: Long, endMillis: Long, customPhotoUrl: String?, budgetAmt: Double?, budgetCurr: String?, isTrackerEnabled: Boolean, trackingMode: String, isOpenEnded: Boolean) -> Unit
) {
    var tripName by remember(existingTripData) { mutableStateOf(existingTripData?.name ?: "") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }

    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = existingTripData?.startDate)

    val startCal = existingTripData?.startDate?.let { java.util.Calendar.getInstance().apply { timeInMillis = it } }
    val startTimePickerState = rememberTimePickerState(
        initialHour = startCal?.get(java.util.Calendar.HOUR_OF_DAY) ?: 9,
        initialMinute = startCal?.get(java.util.Calendar.MINUTE) ?: 0
    )

    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = existingTripData?.endDate)

    val endCal = existingTripData?.endDate?.let { java.util.Calendar.getInstance().apply { timeInMillis = it } }
    val endTimePickerState = rememberTimePickerState(
        initialHour = endCal?.get(java.util.Calendar.HOUR_OF_DAY) ?: 17,
        initialMinute = endCal?.get(java.util.Calendar.MINUTE) ?: 0
    )

    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                customImageUri = uri
            }
        }
    )

    var selectedCurrency by remember(existingTripData) { mutableStateOf("USD") }
    var budgetAmount by remember(existingTripData, exchangeRates) {
        mutableStateOf(
            if (existingTripData != null) {
                if (exchangeRates.isNotEmpty()) {
                    val rate = exchangeRates["VND"] ?: 25425.0
                    val amountInUsd = existingTripData.budget / rate
                    kotlin.math.round(amountInUsd).toInt().toString()
                } else {
                    ""
                }
            } else {
                ""
            }
        )
    }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    // Removed trackingMode
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

    val overlappingTripName = remember(startMillis, endMillis, existingTrips, editTripId) {
        if (startMillis == null || endMillis == null) return@remember null
        existingTrips.find { trip ->
            trip.id != editTripId && startMillis <= trip.endDate && endMillis >= trip.startDate
        }?.name
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (editTripId != null) "Edit trip" else "New trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground // DYNAMIC
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape) // DYNAMIC
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) // DYNAMIC
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary, // DYNAMIC
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // DYNAMIC
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface) // DYNAMIC
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (startMillis != null) {
                            val isOpenEnded = endMillis == null
                            val finalEndMillis = endMillis ?: startMillis
                            val days = if (isOpenEnded) 3 else ((finalEndMillis - startMillis) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0) + 1
                            val photoUrlToPass = if (customImageUri != null) customImageUri.toString() else coverPhotoUrl
                            val bAmt = budgetAmount.toDoubleOrNull()
                            val bCurr = if (bAmt != null) selectedCurrency else null
                            val finalTripName = if (tripName.isNotBlank()) tripName else "Trip to $destinations"
                            onCreateTrip(days, finalTripName, startMillis, finalEndMillis, photoUrlToPass, bAmt, bCurr, false, "None", isOpenEnded)
                        }
                    },
                    enabled = startMillis != null && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // DYNAMIC
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) // DYNAMIC
                        Spacer(modifier = Modifier.width(12.dp))
                        val loadingText = if (editTripId != null) "Saving Settings..." else "Generating Trip..."
                        Text(loadingText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) // DYNAMIC
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (editTripId != null) {
                                Text("Save Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold) // DYNAMIC
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(24.dp)) // DYNAMIC
                            } else {
                                Text("Create Trip with AI", fontSize = 18.sp, fontWeight = FontWeight.Bold) // DYNAMIC
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(painter = painterResource(id = R.drawable.ai), contentDescription = null, modifier = Modifier.size(24.dp))
                            }
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // DYNAMIC
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
                            tint = MaterialTheme.colorScheme.onSurface, // DYNAMIC
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Cover",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // DYNAMIC
                        )
                    }
                }
            }

            // The white "Bottom Sheet" area
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.background, // DYNAMIC
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.pocketplanner.R.drawable.ntpen),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "${tripName.length}/50",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // DYNAMIC
                            focusedBorderColor = MaterialTheme.colorScheme.primary, // DYNAMIC
                            focusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface // DYNAMIC
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Selection Sub-Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface, // DYNAMIC
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Trip dates",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface // DYNAMIC
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // DYNAMIC

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
                                    Text("Start date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        startDateText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (startDateText == "Set date & time") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, // DYNAMIC
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                // Arrow sitting over the dashed line
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "To", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) // DYNAMIC

                                // End Date
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showEndDatePicker = true }
                                ) {
                                    Text("End date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        endDateText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (endDateText == "Optional") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, // DYNAMIC
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    if (overlappingTripName != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer, // DYNAMIC WARNING BACKGROUND
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
                                    tint = MaterialTheme.colorScheme.onErrorContainer, // DYNAMIC
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Warning: This trip overlaps with your existing trip \"$overlappingTripName\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer, // DYNAMIC
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(painter = painterResource(id = R.drawable.ntbudget), contentDescription = "Budget", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC
                        },
                        trailingIcon = {
                            Box {
                                TextButton(onClick = { currencyDropdownExpanded = true }) {
                                    Text(selectedCurrency, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) // DYNAMIC
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Currency", tint = MaterialTheme.colorScheme.primary) // DYNAMIC
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
                            focusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                            focusedBorderColor = MaterialTheme.colorScheme.primary, // DYNAMIC
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant // DYNAMIC
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live VND Conversion
                    if (budgetAmount.isNotEmpty()) {
                        val vndRate = exchangeRates["VND"] ?: 25400.0
                        val selectedRate = exchangeRates[selectedCurrency] ?: 1.0
                        val parsedAmount = budgetAmount.toDoubleOrNull() ?: 0.0
                        val vndAmount = parsedAmount * (vndRate / selectedRate)

                        val formattedVnd = try {
                            val format = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
                            format.maximumFractionDigits = 0
                            format.format(vndAmount)
                        } catch (e: Exception) {
                            "%,.0f".format(Locale.US, vndAmount)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = formattedVnd,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            leadingIcon = {
                                Text("≈", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp)) // DYNAMIC
                            },
                            trailingIcon = {
                                Text("VND", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) // DYNAMIC
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // DYNAMIC
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // DYNAMIC
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC
                                disabledContainerColor = MaterialTheme.colorScheme.surface, // DYNAMIC
                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant // DYNAMIC
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // (Travel Tracker section removed per user request)

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