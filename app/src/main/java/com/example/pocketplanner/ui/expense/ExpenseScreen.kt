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
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.AutoAwesome
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
    onNavigateToHistory: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        viewModel.initialize(tripId, userId)
    }

    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgetState by viewModel.budgetOverview.collectAsState(initial = BudgetState(0.0, 0.0, 0.0))
    val trip by viewModel.currentTrip.collectAsState(initial = null)
    val exchangeRates by viewModel.exchangeRates.collectAsState(initial = emptyMap())

    var showAddSheet by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // DYNAMIC BACKGROUND
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary, // DYNAMIC BUTTON
                contentColor = MaterialTheme.colorScheme.onPrimary, // DYNAMIC ICON
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 92.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 72.dp,
                    bottom = 120.dp
                )
            ) {
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
                            color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (trip != null) {
                            val startStr = dateFormatter.format(Date(trip!!.startDate))
                            val endStr = if (trip!!.isOpenEnded) "Ongoing" else dateFormatter.format(Date(trip!!.endDate))
                            val days = if (trip!!.isOpenEnded) ((System.currentTimeMillis() - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1 else ((trip!!.endDate - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                            Text(
                                text = "$startStr - $endStr • $days Days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
                            )
                        }
                    }
                }

                item {
                    BudgetProgressCard(
                        budgetState = budgetState,
                        currencyFormatter = currencyFormatter
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    CategorySummaryRow(
                        expenses = expenses,
                        currencyFormatter = currencyFormatter
                    )
                }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onNavigateBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                }
            }

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
                        BottomNavPill("Plan", false, Modifier.weight(1f)) { onPlanClick() }
                        BottomNavPill("Expense", true, Modifier.weight(1f)) { }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND (FIXED!)
            modifier = Modifier.fillMaxHeight(),
            dragHandle = null
        ) {
            val context = LocalContext.current
            AddExpenseForm(
                onDismiss = { showAddSheet = false },
                onSave = { amount, category, desc, date -> viewModel.addExpense(category, amount, desc, date)
                    showAddSheet = false
                },
                onScanReceipt = { uri -> viewModel.scanReceipt(uri, context) }
            )
        }
    }
}

@Composable
fun CategoryCard(category: String, total: Double, formatter: NumberFormat) {
    val icon = getIconForCategory(category)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // DYNAMIC BORDER
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), // DYNAMIC BACKGROUND
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) // DYNAMIC ICON
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                Text(formatter.format(total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
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
    onDelete: (ExpenseEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, // DYNAMIC TEXT
            text = { Text("Are you sure you want to delete '${expense.description}'? This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) }, // DYNAMIC TEXT
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) // DYNAMIC TEXT
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.primary) } // DYNAMIC TEXT
            }
        )
    }

    val icon = getIconForCategory(expense.category)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // DYNAMIC BORDER
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = { /* Edit screen trigger */ },
                    onLongClick = { showDeleteDialog = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), // DYNAMIC BACKGROUND
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) // DYNAMIC ICON
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = dateFormatter.format(Date(expense.date))
                Text("$dateStr • ${expense.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "-${formatter.format(expense.amount)}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error // DYNAMIC TEXT
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
    // Semantic chart colors intentionally kept intact for the donut chart
    return when (category.lowercase()) {
        "food" -> Color(0xFF00C853)
        "shopping" -> Color(0xFFE91E63)
        "groceries" -> Color(0xFF9C27B0)
        "transport" -> Color(0xFFFF9800)
        "lodging", "accommodation" -> Color(0xFFFFCA28)
        "gaming" -> Color(0xFF7E57C2)
        "books" -> Color(0xFF009688)
        "bills" -> Color(0xFF2196F3)
        else -> Color(0xFF005b9f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseForm(
    exchangeRates: Map<String, Double> = emptyMap(),
    onSave: (amount: Double, category: String, desc: String, date: Long) -> Unit,
    onDismiss: () -> Unit = {},
    onScanReceipt: suspend (android.net.Uri) -> ScannedReceipt? = { null }
) {
    var rawVndAmount by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0.0) }
    var amountText by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var isUsd by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    var category by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Food") }
    var notes by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var isScanning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    data class CategoryItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

    val categories = listOf(
        CategoryItem("Food", Icons.Filled.LocalDining, Color(0xFF00C853)),
        CategoryItem("Shopping", Icons.Filled.ShoppingBag, Color(0xFFE91E63)),
        CategoryItem("Groceries", Icons.Filled.ShoppingCart, Color(0xFF9C27B0)),
        CategoryItem("Transport", Icons.Filled.DirectionsTransit, Color(0xFFFF9800)),
        CategoryItem("Lodging", Icons.Filled.Hotel, Color(0xFFFFCA28)),
        CategoryItem("Gaming", Icons.Filled.SportsEsports, Color(0xFF7E57C2)),
        CategoryItem("Books", Icons.Filled.MenuBook, Color(0xFF009688)),
        CategoryItem("Bills", Icons.Filled.Receipt, Color(0xFF2196F3))
    )

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

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
                set(java.util.Calendar.HOUR_OF_DAY, now.get(java.util.Calendar.HOUR_OF_DAY))
                set(java.util.Calendar.MINUTE, now.get(java.util.Calendar.MINUTE))
                set(java.util.Calendar.SECOND, now.get(java.util.Calendar.SECOND))
                set(java.util.Calendar.MILLISECOND, now.get(java.util.Calendar.MILLISECOND))
            }.timeInMillis
        } else {
            System.currentTimeMillis()
        }
    }

    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    val displayDate = dateFormatter.format(java.util.Date(selectedMillis))

    val context = LocalContext.current
    var receiptUriString by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraUriString by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    
    val receiptUri = receiptUriString?.let { android.net.Uri.parse(it) }
    val tempCameraUri = tempCameraUriString?.let { android.net.Uri.parse(it) }
    
    var showImageSourceDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val handleScan = { uri: android.net.Uri ->
        coroutineScope.launch {
            isScanning = true
            val result = onScanReceipt(uri)
            if (result != null) {
                isUsd = result.currency == "USD"
                rawVndAmount = result.amount
                
                val vndRate = exchangeRates["VND"] ?: 25000.0
                if (isUsd) {
                    val usdVal = if (vndRate > 0) rawVndAmount / vndRate else 0.0
                    val formattedUsd = String.format(java.util.Locale.US, "%.2f", usdVal)
                    amountText = androidx.compose.ui.text.input.TextFieldValue(text = formattedUsd, selection = androidx.compose.ui.text.TextRange(formattedUsd.length))
                } else {
                    val formatter = java.text.DecimalFormat("#,###")
                    val formattedVnd = formatter.format(rawVndAmount).replace(",", ".")
                    amountText = androidx.compose.ui.text.input.TextFieldValue(text = formattedVnd, selection = androidx.compose.ui.text.TextRange(formattedVnd.length))
                }

                val foundCat = categories.find { it.name.equals(result.category, ignoreCase = true) }
                if (foundCat != null) category = foundCat.name
                if (result.notes.isNotEmpty()) notes = result.notes
            } else {
                android.widget.Toast.makeText(context, "Failed to analyze receipt. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            }
            isScanning = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> 
            if (uri != null) {
                receiptUriString = uri.toString()
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> 
            if (success && tempCameraUri != null) {
                receiptUriString = tempCameraUri.toString()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // DYNAMIC BACKGROUND
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) // DYNAMIC ICON
            }
            Text(
                "Add Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, // DYNAMIC TEXT
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
                color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                    Spacer(modifier = Modifier.height(16.dp))

                    val numberFormatter = remember { java.text.DecimalFormat("#,###") }
                    val vndRate = exchangeRates["VND"] ?: 25000.0

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
                            fontSize = dynamicFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT
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
                                fontSize = dynamicFontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // DYNAMIC TEXT
                            ),
                            decorationBox = { innerTextField ->
                                if (amountText.text.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = dynamicFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // DYNAMIC TEXT
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant, // DYNAMIC BACKGROUND
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
                color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                    Spacer(modifier = Modifier.height(12.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat.name
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant // DYNAMIC BORDER
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { category = cat.name }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 12.dp)
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
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayDate,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Date", tint = MaterialTheme.colorScheme.onSurfaceVariant) }, // DYNAMIC ICON
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
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

                    Text("Receipt / Image", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                    Spacer(modifier = Modifier.height(8.dp))

                    if (receiptUri == null) {
                        val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) }
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = outlineColor, // DYNAMIC BORDER
                                        style = Stroke(width = 4f, pathEffect = dashPathEffect),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showImageSourceDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) { // DYNAMIC BACKGROUND
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp)) // DYNAMIC ICON
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to upload receipt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upload a receipt to auto-fill with AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                AsyncImage(
                                    model = receiptUri,
                                    contentDescription = "Receipt",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                if (!isScanning) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clickable { receiptUriString = null }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.padding(4.dp).size(20.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Scanning receipt...", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { 
                                    receiptUri?.let { handleScan(it) } 
                                },
                                enabled = !isScanning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning...", color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Auto-fill with AI", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Notes (Optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add any extra details here...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }, // DYNAMIC TEXT
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalDescription = if (notes.isNotBlank()) "$category - $notes" else category
                    onSave(rawVndAmount, category, finalDescription, selectedMillis)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // DYNAMIC BACKGROUND
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) // DYNAMIC ICON
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) // DYNAMIC TEXT
            }
        }
    }

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

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
            title = { Text("Upload Receipt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, // DYNAMIC TEXT
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
                        }
                    }
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            tempCameraUriString = uri.toString()
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) // DYNAMIC ICON
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Take a Photo", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) // DYNAMIC TEXT
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

@Composable
fun CurrencyToggleBtn(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, // DYNAMIC BACKGROUND
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
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}
