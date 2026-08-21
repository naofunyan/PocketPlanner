package com.example.pocketplanner.ui.expense

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.text.DecimalFormat
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.io.File
import coil.compose.AsyncImage
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onPlanClick: () -> Unit,
    onTrackClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    // 1. Initialize ViewModel with the specific tripId
    LaunchedEffect(tripId) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        viewModel.initialize(tripId, userId)
    }

    // 2. Observe the new reactive state flows directly
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgetState by viewModel.budgetOverview.collectAsState(initial = BudgetState(0.0, 0.0, 0.0))
    val trip by viewModel.currentTrip.collectAsState(initial = null)
    val exchangeRates by viewModel.exchangeRates.collectAsState(initial = emptyMap())

    var showAddSheet by remember { mutableStateOf(false) }

    // Formatter for VND
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFF005b9f), // Primary Blue
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 92.dp) // Lift above custom bottom nav
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // Main Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 72.dp,
                    bottom = 120.dp
                )
            ) {
                // 1. DYNAMIC TITLE HEADER
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = trip?.let {
                                if (it.name.isNotBlank()) it.name else "Trip to ${it.destination}"
                            } ?: "Loading...",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (trip != null) {
                            val startStr = dateFormatter.format(Date(trip!!.startDate))
                            val endStr = dateFormatter.format(Date(trip!!.endDate))
                            val days = ((trip!!.endDate - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                            Text(
                                text = "$startStr - $endStr • $days Days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 2. BUDGET CARD
                item {
                    BudgetProgressCard(
                        budgetState = budgetState,
                        currencyFormatter = currencyFormatter
                    )
                }

                // 3. CATEGORIES ROW
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    CategorySummaryRow(
                        expenses = expenses,
                        currencyFormatter = currencyFormatter
                    )
                }

                // 4. TRANSACTIONS WIDGET (Max 5 items)
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    TransactionListWidget(
                        expenses = expenses,
                        currencyFormatter = currencyFormatter,
                        dateFormatter = dateFormatter,
                        onViewMoreClick = {
                            onNavigateToHistory()
                        },
                        onDeleteClick = { expenseToDelete -> viewModel.deleteExpense(expenseToDelete) }
                    )
                }
            }

            // Top controls overlay (Back button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onNavigateBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp), tint = Color.DarkGray)
                }
            }

            // Custom Pill Bottom Navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFFF2F2F2),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavPill("Plan", false, Modifier.weight(1f)) { onPlanClick() }
                        BottomNavPill("Expense", true, Modifier.weight(1f)) { }
                        BottomNavPill("Track", false, Modifier.weight(1f)) { onTrackClick() }
                    }
                }
            }
        }
    }

    // Add Expense Bottom Sheet
    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight(), // 1. Force it to stretch all the way to the top!
            dragHandle = null // 2. Hides the default grey drag bar since you have a custom back arrow
        ) {
            AddExpenseForm(
                onDismiss = { showAddSheet = false },
                onSave = { amount, category, desc, date -> viewModel.addExpense(category, amount, desc, date)
                    showAddSheet = false
                }
            )
        }
    }
}

@Composable
fun CategoryCard(category: String, total: Double, formatter: NumberFormat) {
    val icon = getIconForCategory(category)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFAFAFA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(category, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(formatter.format(total), fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    expense: ExpenseEntity,
    formatter: java.text.NumberFormat,
    dateFormatter: java.text.SimpleDateFormat,
    onDelete: (ExpenseEntity) -> Unit // 1. Added an onDelete callback!
) {
    // 2. State to track if the dialog is open
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 3. The Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${expense.description}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense)
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val icon = getIconForCategory(expense.category)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = { /* You can add an edit screen trigger here later! */ },
                    onLongClick = { showDeleteDialog = true } // 4. Triggers the dialog on long press
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF0F0F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = dateFormatter.format(Date(expense.date))
                Text("$dateStr • ${expense.category}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "-${formatter.format(expense.amount)}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

fun getIconForCategory(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Filled.LocalDining
        "transport" -> Icons.Filled.DirectionsTransit
        "accommodation", "lodging" -> Icons.Filled.Hotel
        "activities" -> Icons.Filled.LocalActivity
        "shopping" -> Icons.Filled.ShoppingBag
        else -> Icons.Filled.Wallet
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> Color(0xFF00C853)      // Green
        "shopping" -> Color(0xFFE91E63)  // Pink
        "groceries" -> Color(0xFF9C27B0) // Purple
        "transport" -> Color(0xFFFF9800) // Orange
        "lodging", "accommodation" -> Color(0xFFFFCA28) // Yellow
        "gaming" -> Color(0xFF7E57C2)    // Deep Purple
        "books" -> Color(0xFF009688)     // Teal
        "bills" -> Color(0xFF2196F3)     // Blue
        else -> Color(0xFF005b9f)        // Default App Blue
    }
}

@Composable
fun BottomNavPill(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color(0xFF4694DA) else Color.Transparent,
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
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    exchangeRates: Map<String, Double> = emptyMap(),
    onSave: (amount: Double, category: String, desc: String, date: Long) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var rawVndAmount by remember { mutableStateOf(0.0) }
    var amountText by remember { mutableStateOf(TextFieldValue("")) }
    var isUsd by remember { mutableStateOf(false) }

    var category by remember { mutableStateOf("Food") }
    var notes by remember { mutableStateOf("") }

    // Helper class to hold the color along with the name and icon
    data class CategoryItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

    val categories = listOf(
        CategoryItem("Food", Icons.Filled.LocalDining, Color(0xFF00C853)),       // Green
        CategoryItem("Shopping", Icons.Filled.ShoppingBag, Color(0xFFE91E63)),   // Pink
        CategoryItem("Groceries", Icons.Filled.ShoppingCart, Color(0xFF9C27B0)), // Purple
        CategoryItem("Transport", Icons.Filled.DirectionsTransit, Color(0xFFFF9800)), // Orange
        CategoryItem("Lodging", Icons.Filled.Hotel, Color(0xFFFFCA28)),          // Yellow
        CategoryItem("Gaming", Icons.Filled.SportsEsports, Color(0xFF7E57C2)),   // Deep Purple
        CategoryItem("Books", Icons.Filled.MenuBook, Color(0xFF009688)),         // Teal
        CategoryItem("Bills", Icons.Filled.Receipt, Color(0xFF2196F3))           // Blue
    )

    // --- DATE STATE ---
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // 1. Grab the selected date and inject the current exact time so sorting works perfectly!
    val selectedMillis = remember(datePickerState.selectedDateMillis) {
        val selectedUtc = datePickerState.selectedDateMillis
        if (selectedUtc != null) {
            val now = java.util.Calendar.getInstance()
            val selectedCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedUtc
            }

            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, selectedCalendar.get(java.util.Calendar.YEAR))
                set(java.util.Calendar.MONTH, selectedCalendar.get(java.util.Calendar.MONTH))
                set(java.util.Calendar.DAY_OF_MONTH, selectedCalendar.get(java.util.Calendar.DAY_OF_MONTH))
                // Retain the exact current time for accurate reverse-chronological sorting
                set(java.util.Calendar.HOUR_OF_DAY, now.get(java.util.Calendar.HOUR_OF_DAY))
                set(java.util.Calendar.MINUTE, now.get(java.util.Calendar.MINUTE))
                set(java.util.Calendar.SECOND, now.get(java.util.Calendar.SECOND))
                set(java.util.Calendar.MILLISECOND, now.get(java.util.Calendar.MILLISECOND))
            }.timeInMillis
        } else {
            System.currentTimeMillis()
        }
    }

    // 2. Format to a clean date string like "Aug 21, 2026"
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    val displayDate = dateFormatter.format(java.util.Date(selectedMillis)) // <-- Re-added this missing line!

    // --- RECEIPT IMAGE STATE ---
    val context = LocalContext.current
    var receiptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Launcher for picking from Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) receiptUri = uri }
    )

    // Launcher for taking a Photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) receiptUri = tempCameraUri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FA)) // Soft gray-blue background
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
                "Add Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- AMOUNT CARD ---
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    val numberFormatter = remember { java.text.DecimalFormat("#,###") }
                    val vndRate = exchangeRates["VND"] ?: 25000.0

                    // Automatically convert and update the text field when the user taps the toggle button
                    LaunchedEffect(isUsd) {
                        if (rawVndAmount > 0.0) {
                            if (isUsd) {
                                val usdVal = if (vndRate > 0) rawVndAmount / vndRate else 0.0
                                val formattedUsd = String.format(Locale.US, "%.2f", usdVal)
                                amountText = TextFieldValue(text = formattedUsd, selection = TextRange(formattedUsd.length))
                            } else {
                                val formattedVnd = numberFormatter.format(rawVndAmount).replace(",", ".")
                                amountText = TextFieldValue(text = formattedVnd, selection = TextRange(formattedVnd.length))
                            }
                        } else {
                            amountText = TextFieldValue("")
                        }
                    }

                    // 1. Dynamically shrink the font size as the number gets longer!
                    val dynamicFontSize = when {
                        amountText.text.length >= 15 -> 28.sp
                        amountText.text.length >= 12 -> 36.sp
                        amountText.text.length >= 11 -> 42.sp
                        else -> 48.sp
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isUsd) "$" else "₫",
                            fontSize = dynamicFontSize, // 2. Shrinks the currency symbol to match
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        androidx.compose.foundation.text.BasicTextField(
                            value = amountText,
                            onValueChange = { newValue ->
                                val cleanString = newValue.text.replace(",", "").replace(".", "")

                                if (cleanString.isEmpty()) {
                                    amountText = TextFieldValue("")
                                    rawVndAmount = 0.0
                                } else {
                                    if (isUsd) {
                                        val parsedUsd = newValue.text.toDoubleOrNull() ?: 0.0
                                        rawVndAmount = parsedUsd * vndRate
                                        amountText = newValue
                                    } else {
                                        val parsedVnd = cleanString.toDoubleOrNull() ?: 0.0
                                        rawVndAmount = parsedVnd
                                        val formatted = numberFormatter.format(parsedVnd.toLong())
                                        val finalString = formatted.replace(",", ".")
                                        amountText = TextFieldValue(
                                            text = finalString,
                                            selection = TextRange(finalString.length)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = dynamicFontSize, // 3. Applies the shrinking size to the input
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005b9f)
                            ),
                            decorationBox = { innerTextField ->
                                if (amountText.text.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = dynamicFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005b9f).copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Currency Toggle Buttons
                    Surface(
                        color = Color(0xFFF0F4F8),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            CurrencyToggleBtn("VND", !isUsd) { isUsd = false }
                            CurrencyToggleBtn("USD", isUsd) { isUsd = true }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- DETAILS CARD ---
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Category
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2, // 1. Forces a perfect 2-column grid
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat.name
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF005b9f) else Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier
                                    .weight(1f) // 2. Stretches the chip to the left and right edges!
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { category = cat.name }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center, // 3. Keeps the icon and text perfectly centered inside the stretched chip
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 12.dp) // Adjusted padding for a taller, more tappable area
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = cat.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color(0xFF005b9f) else Color.DarkGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Date
                    Text("Date", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayDate, // Uses the simplified date format
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Receipt Upload Box
                    Text("Receipt / Image", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (receiptUri == null) {
                        val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color(0xFFB0BEC5),
                                        style = Stroke(width = 4f, pathEffect = dashPathEffect),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Removes the grey ripple for a cleaner tap
                                ) {
                                    showImageSourceDialog = true // <-- Opens the Camera/Gallery dialog!
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = CircleShape, color = Color(0xFFE3F2FD), modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to upload receipt", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    } else {
                        // The Selected Image State
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            AsyncImage(
                                model = receiptUri,
                                contentDescription = "Receipt",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)) // Matched to your 16.dp corner radius
                            )
                            // Remove button overlay
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clickable { receiptUri = null } // Clears the image and brings back the dashed box
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.padding(4.dp).size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Notes
                    Text("Notes (Optional)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add any extra details here...", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    // 1. Use the category as the base name, appending notes if they exist!
                    val finalDescription = if (notes.isNotBlank()) "$category - $notes" else category

                    // 2. Directly save the rawVndAmount state!
                    onSave(rawVndAmount, category, finalDescription, selectedMillis)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b9f))
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOGS ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                // Changed from "Next" to "OK", and just closes the dialog
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Upload Receipt", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery", fontSize = 16.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            // Create a temporary file to store the camera photo
                            val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
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

// Helper for the USD/VND switch
@Composable
fun CurrencyToggleBtn(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF005b9f) else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}