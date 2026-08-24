package com.example.pocketplanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.roundToInt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Collect all states
    val name by viewModel.medicalName.collectAsState()
    val conditions by viewModel.medicalConditions.collectAsState()
    val bloodType by viewModel.medicalBloodType.collectAsState()
    val allergies by viewModel.medicalAllergies.collectAsState()
    val medications by viewModel.medicalMedications.collectAsState()
    val weight by viewModel.medicalWeight.collectAsState()
    val height by viewModel.medicalHeight.collectAsState()
    val dob by viewModel.medicalDob.collectAsState()
    val address by viewModel.medicalAddress.collectAsState()
    val organDonor by viewModel.medicalOrganDonor.collectAsState()
    val notes by viewModel.medicalNotes.collectAsState()

    // NEW: Collect the toggle state
    val shareDuringEmergencyEnabled by viewModel.shareDuringEmergencyEnabled.collectAsState()

    // Dynamic Dialog State
    var showEditDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogValue by remember { mutableStateOf("") }
    var currentKey by remember { mutableStateOf<Preferences.Key<String>?>(null) }
    // Blood Type Dialog State
    val bloodTypes = listOf("Not set", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var showBloodTypeDialog by remember { mutableStateOf(false) }
    var tempBloodType by remember { mutableStateOf(bloodType) }
    // Weight Dialog State
    var showWeightDialog by remember { mutableStateOf(false) }
    // Height Dialog State
    var showHeightDialog by remember { mutableStateOf(false) }
    // Date of Birth Dialog State
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    // Organ Donor Dialog State
    val organDonorOptions = listOf("Not set", "Yes", "No")
    var showOrganDonorDialog by remember { mutableStateOf(false) }
    var tempOrganDonor by remember { mutableStateOf(organDonor) }

    val openDialog = { title: String, value: String, key: Preferences.Key<String> ->
        dialogTitle = title
        dialogValue = value
        currentKey = key
        showEditDialog = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Medical info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Top Illustration Card
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            // 2. Explanatory Text
            Text(
                text = "Record your allergies, current medication, and other medical information so it's available in an emergency.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 3. Medical Info Fields Card (Bottom padding removed from here)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {

                    MedicalInfoItem(
                        icon = Icons.Default.PersonOutline, label = "Name",
                        text = name.ifEmpty { "Enter your name" }, isHint = name.isEmpty(),
                        onClick = { openDialog("Name", name, SettingsViewModel.MEDICAL_NAME) }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Assignment, label = "Medical conditions",
                        text = conditions.ifEmpty { "List your medical conditions" }, isHint = conditions.isEmpty(),
                        onClick = { openDialog("Medical conditions", conditions, SettingsViewModel.MEDICAL_CONDITIONS) }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.WaterDrop, label = "Blood type",
                        text = bloodType.ifEmpty { "Tap to select blood type" },
                        isHint = bloodType.isEmpty(),
                        onClick = {
                            tempBloodType = bloodType // Reset to current selection
                            showBloodTypeDialog = true
                        }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Block, label = "Allergies",
                        text = allergies.ifEmpty { "List your allergies" }, isHint = allergies.isEmpty(),
                        onClick = { openDialog("Allergies", allergies, SettingsViewModel.MEDICAL_ALLERGIES) }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Medication, label = "Current medications",
                        text = medications.ifEmpty { "List any medication you take" }, isHint = medications.isEmpty(),
                        onClick = { openDialog("Current medications", medications, SettingsViewModel.MEDICAL_MEDICATIONS) }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.MonitorWeight, label = "Weight",
                        text = weight.ifEmpty { "Tap to set weight" }, isHint = weight.isEmpty(),
                        onClick = { showWeightDialog = true } // <-- UPDATED
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Height, label = "Height",
                        text = height.ifEmpty { "Tap to set height" }, isHint = height.isEmpty(),
                        onClick = { showHeightDialog = true } // <-- UPDATED
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.CalendarToday, label = "Date of birth",
                        text = dob.ifEmpty { "Tap to set date" }, isHint = dob.isEmpty(),
                        onClick = { showDatePickerDialog = true } // <-- UPDATED
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Home, label = "Address",
                        text = address.ifEmpty { "Enter your address" }, isHint = address.isEmpty(),
                        onClick = { openDialog("Address", address, SettingsViewModel.MEDICAL_ADDRESS) }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.FavoriteBorder, label = "Organ donor",
                        text = organDonor.ifEmpty { "Tap to set status" },
                        isHint = organDonor.isEmpty(),
                        onClick = {
                            tempOrganDonor = organDonor // Reset to current selection
                            showOrganDonorDialog = true
                        }
                    )

                    MedicalInfoItem(
                        icon = Icons.Default.Notes, label = "Medical notes",
                        text = notes.ifEmpty { "Enter other important info" }, isHint = notes.isEmpty(), showDivider = false,
                        onClick = { openDialog("Medical notes", notes, SettingsViewModel.MEDICAL_NOTES) }
                    )
                }
            }

            // 4. Share during emergency toggle
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share with emergency contacts", // <-- UPDATED TITLE
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Append your critical medical info to the automated SOS texts sent to your designated emergency contacts.", // <-- UPDATED DESCRIPTION
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = shareDuringEmergencyEnabled,
                        onCheckedChange = { viewModel.updateShareDuringEmergency(it) }
                    )
                }
            }
        }
    }

    // --- SMART EDIT DIALOG ---
    if (showEditDialog && currentKey != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = dialogTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                OutlinedTextField(
                    value = dialogValue,
                    onValueChange = { dialogValue = it },
                    label = { Text("Enter details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (dialogTitle.contains("notes", true) || dialogTitle.contains("conditions", true)) 3 else 1
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMedicalField(currentKey!!, dialogValue)
                        showEditDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // --- BLOOD TYPE RADIO DIALOG ---
    if (showBloodTypeDialog) {
        AlertDialog(
            onDismissRequest = { showBloodTypeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = "Blood type", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    bloodTypes.forEach { type ->
                        // Map "Not set" to an empty string to keep our hint logic working
                        val actualValue = if (type == "Not set") "" else type
                        val isSelected = tempBloodType == actualValue

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempBloodType = actualValue }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempBloodType = actualValue }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = type,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMedicalField(SettingsViewModel.MEDICAL_BLOOD_TYPE, tempBloodType)
                        showBloodTypeDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBloodTypeDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // --- WEIGHT WHEEL DIALOG ---
    if (showWeightDialog) {
        WeightPickerDialog(
            initialWeight = weight,
            onDismiss = { showWeightDialog = false },
            onSave = { newWeight ->
                viewModel.updateMedicalField(SettingsViewModel.MEDICAL_WEIGHT, newWeight)
                showWeightDialog = false
            }
        )
    }

    // --- HEIGHT WHEEL DIALOG ---
    if (showHeightDialog) {
        HeightPickerDialog(
            initialHeight = height,
            onDismiss = { showHeightDialog = false },
            onSave = { newHeight ->
                viewModel.updateMedicalField(SettingsViewModel.MEDICAL_HEIGHT, newHeight)
                showHeightDialog = false
            }
        )
    }

    // --- NATIVE DATE PICKER DIALOG ---
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Convert the raw milliseconds into a readable date string
                            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val formattedDate = formatter.format(Date(millis))
                            viewModel.updateMedicalField(SettingsViewModel.MEDICAL_DOB, formattedDate)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false // Hides the manual text-entry icon so it stays strictly as a calendar
            )
        }
    }

    // --- ORGAN DONOR RADIO DIALOG ---
    if (showOrganDonorDialog) {
        AlertDialog(
            onDismissRequest = { showOrganDonorDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = "Organ donor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    organDonorOptions.forEach { option ->
                        // Map "Not set" to an empty string to keep our hint logic working
                        val actualValue = if (option == "Not set") "" else option
                        val isSelected = tempOrganDonor == actualValue

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempOrganDonor = actualValue }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempOrganDonor = actualValue }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMedicalField(SettingsViewModel.MEDICAL_ORGAN_DONOR, tempOrganDonor)
                        showOrganDonorDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrganDonorDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun MedicalInfoItem(
    icon: ImageVector,
    label: String,
    text: String,
    isHint: Boolean = false,
    showDivider: Boolean = true,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp).size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = if (isHint) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeightPickerDialog(
    initialWeight: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // 1. Parse the initial string (e.g. "65.0 kg") safely, defaulting to 65.0 kg
    val parts = initialWeight.trim().split(" ")
    val initialTotal = parts.getOrNull(0)?.toFloatOrNull() ?: 65.0f
    val initialUnit = parts.getOrNull(1) ?: "kg"

    val initialWhole = initialTotal.toInt()
    val initialDec = ((initialTotal - initialWhole) * 10).roundToInt().coerceIn(0, 9)

    // 2. Define our scrollable options
    val wholeNumbers = (0..500).toList()
    val decimals = (0..9).toList()
    val units = listOf("kg", "lb")

    val wholePagerState = rememberPagerState(initialPage = initialWhole) { wholeNumbers.size }
    val decimalPagerState = rememberPagerState(initialPage = initialDec) { decimals.size }
    val unitPagerState = rememberPagerState(initialPage = units.indexOf(initialUnit).coerceAtLeast(0)) { units.size }

    // 3. Handle live conversion when the unit changes
    var currentLogicUnit by remember { mutableStateOf(units[unitPagerState.currentPage]) }

    LaunchedEffect(unitPagerState.currentPage) {
        val selectedUnit = units[unitPagerState.currentPage]

        if (selectedUnit != currentLogicUnit) {
            val currentWhole = wholePagerState.currentPage
            val currentDecimal = decimalPagerState.currentPage
            val currentTotal = currentWhole + (currentDecimal / 10f)

            // Convert between kg (1) and lb (2.20462)
            val newTotal = if (selectedUnit == "lb" && currentLogicUnit == "kg") {
                currentTotal * 2.20462f
            } else if (selectedUnit == "kg" && currentLogicUnit == "lb") {
                currentTotal / 2.20462f
            } else {
                currentTotal
            }

            val newWholeTarget = newTotal.toInt().coerceIn(0, 500)
            val newDecimalTarget = ((newTotal - newWholeTarget) * 10).roundToInt().coerceIn(0, 9)

            currentLogicUnit = selectedUnit

            // Snap the number wheels to the newly converted values instantly
            wholePagerState.scrollToPage(newWholeTarget)
            decimalPagerState.scrollToPage(newDecimalTarget)
        }
    }

    // 4. Build the UI
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = "Set weight", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Whole Numbers Wheel
                WheelPicker(
                    items = wholeNumbers,
                    pagerState = wholePagerState,
                    modifier = Modifier.weight(1.2f)
                )

                // Fixed Decimal Point
                Text(
                    text = ".",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Decimals Wheel
                WheelPicker(
                    items = decimals,
                    pagerState = decimalPagerState,
                    modifier = Modifier.weight(0.8f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Units Wheel (kg / lb)
                WheelPicker(
                    items = units,
                    pagerState = unitPagerState,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalString = "${wholePagerState.currentPage}.${decimalPagerState.currentPage} ${units[unitPagerState.currentPage]}"
                    onSave(finalString)
                }
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/**
 * Reusable Wheel Picker matching the Samsung OneUI aesthetics.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 56.dp), // Pads top and bottom so items center
        horizontalAlignment = Alignment.CenterHorizontally
    ) { page ->
        val isSelected = pagerState.currentPage == page
        Text(
            text = items[page].toString(),
            fontSize = if (isSelected) 26.sp else 22.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeightPickerDialog(
    initialHeight: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // 1. Parse the initial string (e.g., "170.0 cm" or "5'7 ft, in")
    val isInitialFt = initialHeight.contains("ft, in")
    val initialUnit = if (isInitialFt) "ft, in" else "cm"

    val numStr = initialHeight.replace("cm", "").replace("ft, in", "").trim()
    val separator = if (numStr.contains(".")) "." else if (numStr.contains("'")) "'" else " "
    val parts = numStr.split(separator).map { it.trim() }.filter { it.isNotBlank() }

    val initialLeft = parts.getOrNull(0)?.toIntOrNull() ?: if (isInitialFt) 5 else 170
    val initialRight = parts.getOrNull(1)?.toIntOrNull() ?: if (isInitialFt) 7 else 0

    val units = listOf("cm", "ft, in")

    // 2. Track the active unit to dynamically resize the wheels
    var currentLogicUnit by remember { mutableStateOf(initialUnit) }

    val leftCount = if (currentLogicUnit == "cm") 301 else 13 // 0-300cm OR 0-12ft
    val rightCount = if (currentLogicUnit == "cm") 10 else 12 // 0-9 decimals OR 0-11 inches

    val leftPagerState = rememberPagerState(initialPage = initialLeft.coerceIn(0, leftCount - 1)) { leftCount }
    val rightPagerState = rememberPagerState(initialPage = initialRight.coerceIn(0, rightCount - 1)) { rightCount }
    val unitPagerState = rememberPagerState(initialPage = units.indexOf(initialUnit).coerceAtLeast(0)) { units.size }

    // 3. Handle live conversion when the unit wheel changes
    LaunchedEffect(unitPagerState.currentPage) {
        val selectedUnit = units[unitPagerState.currentPage]

        if (selectedUnit != currentLogicUnit) {
            val currentLeft = leftPagerState.currentPage
            val currentRight = rightPagerState.currentPage

            if (selectedUnit == "ft, in" && currentLogicUnit == "cm") {
                val totalCm = currentLeft + (currentRight / 10f)
                val totalIn = totalCm / 2.54f
                val newFt = (totalIn / 12).toInt().coerceIn(0, 12)
                val newIn = (totalIn % 12).roundToInt().coerceIn(0, 11)

                currentLogicUnit = selectedUnit
                kotlinx.coroutines.delay(50) // Safely allow pager counts to recompose
                leftPagerState.scrollToPage(newFt)
                rightPagerState.scrollToPage(newIn)

            } else if (selectedUnit == "cm" && currentLogicUnit == "ft, in") {
                val totalIn = currentLeft * 12 + currentRight
                val totalCm = totalIn * 2.54f
                val newCm = totalCm.toInt().coerceIn(0, 300)
                val newDec = ((totalCm - newCm) * 10).roundToInt().coerceIn(0, 9)

                currentLogicUnit = selectedUnit
                kotlinx.coroutines.delay(50)
                leftPagerState.scrollToPage(newCm)
                rightPagerState.scrollToPage(newDec)
            }
        }
    }

    // 4. Build the UI
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = "Set height", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Whole Numbers / Feet Wheel
                WheelPicker(
                    items = (0 until leftCount).toList(),
                    pagerState = leftPagerState,
                    modifier = Modifier.weight(1.2f)
                )

                // Dynamic Separator (. for cm, ' for ft)
                Text(
                    text = if (currentLogicUnit == "cm") "." else "'",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Decimals / Inches Wheel
                WheelPicker(
                    items = (0 until rightCount).toList(),
                    pagerState = rightPagerState,
                    modifier = Modifier.weight(0.8f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Units Wheel (cm / ft, in)
                WheelPicker(
                    items = units,
                    pagerState = unitPagerState,
                    modifier = Modifier.weight(1.4f) // Slightly wider to fit "ft, in"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sep = if (currentLogicUnit == "cm") "." else "'"
                    val finalString = "${leftPagerState.currentPage}$sep${rightPagerState.currentPage} ${units[unitPagerState.currentPage]}"
                    onSave(finalString)
                }
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}