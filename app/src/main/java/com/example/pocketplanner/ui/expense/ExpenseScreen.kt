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
import coil.compose.rememberAsyncImagePainter
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onPlanClick: () -> Unit,
    onTrackClick: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expenses by viewModel.getExpenses(tripId).collectAsState(initial = emptyList())
    val budgetState by viewModel.getBudgetOverview(tripId).collectAsState(initial = BudgetState(0.0, 0.0, 0.0))
    val trip by viewModel.getTrip(tripId).collectAsState(initial = null)

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
                modifier = Modifier.padding(bottom = 70.dp) // Lift above custom bottom nav
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Map Header Placeholder
            Image(
                painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=800&auto=format&fit=crop"), // Map-like image
                contentDescription = "Map Header",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            
            // Top controls overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(40.dp).clickable(onClick = onNavigateBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp), tint = Color.DarkGray)
                }
            }

            // Main Content Sheet (Overlapping the map)
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFFFAFAFA),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 220.dp) // Push down to let map show
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp) // space for bottom nav
                ) {
                    item {
                        // Title
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                            Text(
                                text = trip?.destination ?: "Loading...",
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

                    item {
                        // Budget Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("TOTAL SPENT", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                currencyFormatter.format(budgetState.totalSpent),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E1E1E)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                " / ${currencyFormatter.format(budgetState.totalBudget)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE3F2FD), // Light blue
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Wallet, contentDescription = null, tint = Color(0xFF005b9f), modifier = Modifier.padding(12.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                val progress = if (budgetState.totalBudget > 0) (budgetState.totalSpent / budgetState.totalBudget).toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = if (progress > 0.9f) Color(0xFFD32F2F) else Color(0xFF005b9f),
                                    trackColor = Color(0xFFEEEEEE)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val percentStr = "${(progress * 100).toInt()}%"
                                Text(
                                    "$percentStr of budget used",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF005b9f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Calculate Category Totals
                        val categoryTotals = expenses.groupBy { it.category }
                            .mapValues { it.value.sumOf { exp -> exp.amount } }
                            .toList()
                            .sortedByDescending { it.second }
                            
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (categoryTotals.isEmpty()) {
                                item { Text("No expenses yet", color = Color.Gray) }
                            }
                            items(categoryTotals) { (cat, total) ->
                                CategoryCard(category = cat, total = total, formatter = currencyFormatter)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                            Text("See All", color = Color(0xFF005b9f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (expenses.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No transactions logged yet.", color = Color.Gray)
                            }
                        }
                    } else {
                        // Transactions list
                        items(expenses.sortedByDescending { it.date }) { expense ->
                            TransactionRow(expense, currencyFormatter, dateFormatter)
                        }
                    }
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
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = Color.White) {
            AddExpenseForm(
                onSave = { amount, category, desc ->
                    viewModel.addExpense(tripId, category, amount, desc)
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

@Composable
fun TransactionRow(expense: ExpenseEntity, formatter: NumberFormat, dateFormatter: SimpleDateFormat) {
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
            modifier = Modifier.padding(16.dp),
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
                color = Color(0xFFD32F2F) // Red for expense
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

// Reused from ItineraryScreen
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

// Ensure you keep your AddExpenseForm unmodified below...
@Composable
fun AddExpenseForm(onSave: (amount: Double, category: String, desc: String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }

    val categories = listOf("Food", "Transport", "Accommodation", "Activities", "Shopping")

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text("Add New Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (VND)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("What was it for?") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(3).forEach { cat ->
                FilterChip(
                    selected = (category == cat),
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.drop(3).forEach { cat ->
                FilterChip(
                    selected = (category == cat),
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onSave(amount.toDoubleOrNull() ?: 0.0, category, description) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b9f))
        ) {
            Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}