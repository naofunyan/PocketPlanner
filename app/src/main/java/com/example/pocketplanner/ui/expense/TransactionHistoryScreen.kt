package com.example.pocketplanner.ui.expense

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        viewModel.initialize(tripId, userId)
    }

    val expenses by viewModel.expenses.collectAsState(initial = emptyList())

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    // Formatter for the Row items (can format to time or just date)
    val itemDateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    // Formatter specifically for grouping headers (e.g., "August 21, 2026")
    val headerDateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }

    // Group the expenses by the formatted date string
    val groupedExpenses = remember(expenses) {
        expenses
            .sortedByDescending { it.date }
            .groupBy { headerDateFormatter.format(Date(it.date)) }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Surface(
                color = Color(0xFFFAFAFA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onNavigateBack)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions logged yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 40.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            ) {
                // Loop through our grouped map: Key is the Date String, Value is the List of Expenses
                groupedExpenses.forEach { (dateString, dailyExpenses) ->

                    // 1. The Date Header
                    item {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                        )
                    }

                    // 2. The Grouped Card for that specific day
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5F5F5))
                        ) {
                            Column {
                                dailyExpenses.forEachIndexed { index, expense ->
                                    // --> ADDED THE ONDELETE COMMAND HERE:
                                    TransactionRowItem(
                                        expense = expense,
                                        formatter = currencyFormatter,
                                        dateFormatter = itemDateFormatter,
                                        onDelete = { expenseToDelete -> viewModel.deleteExpense(expenseToDelete) }
                                    )

                                    if (index < dailyExpenses.lastIndex) {
                                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}