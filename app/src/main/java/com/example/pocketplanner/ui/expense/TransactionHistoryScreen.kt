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
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

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
    val itemDateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val headerDateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }

    val groupedExpenses = remember(expenses) {
        expenses
            .sortedByDescending { it.date }
            .groupBy { headerDateFormatter.format(Date(it.date)) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // <-- DYNAMIC BACKGROUND
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background, // <-- DYNAMIC BACKGROUND
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
                        color = MaterialTheme.colorScheme.surface, // <-- DYNAMIC SURFACE
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onNavigateBack)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.expense_back_cd),
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSurface // <-- DYNAMIC ICON
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.expense_transaction_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground // <-- DYNAMIC TEXT
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
                Text(stringResource(R.string.expense_no_transactions), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                groupedExpenses.forEach { (dateString, dailyExpenses) ->
                    item {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface, // <-- DYNAMIC CARD
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                dailyExpenses.forEachIndexed { index, expense ->
                                    TransactionRowItem(
                                        expense = expense,
                                        formatter = currencyFormatter,
                                        dateFormatter = itemDateFormatter,
                                        onDelete = { expenseToDelete -> viewModel.deleteExpense(expenseToDelete) }
                                    )

                                    if (index < dailyExpenses.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
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