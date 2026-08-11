package com.example.pocketplanner.ui.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.data.local.entity.ExpenseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expenses by viewModel.getExpenses(tripId).collectAsState(initial = emptyList())
    val budgetState by viewModel.getBudgetOverview(tripId).collectAsState(initial = BudgetState(0.0, 0.0, 0.0))

    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Expenses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

            // 1. Budget Overview Card
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Budget Remaining", style = MaterialTheme.typography.titleMedium)
                    Text("${budgetState.remaining} VND", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val progress = if (budgetState.totalBudget > 0) (budgetState.totalSpent / budgetState.totalBudget).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Spent: ${budgetState.totalSpent} / ${budgetState.totalBudget} VND", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 2. Expense List
            Text("Recent Expenses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            if (expenses.isEmpty()) {
                Text("No expenses logged yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses) { expense ->
                        ExpenseItemRow(expense)
                    }
                }
            }
        }
    }

    // 3. Add Expense Bottom Sheet
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
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
fun ExpenseItemRow(expense: ExpenseEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(expense.description, style = MaterialTheme.typography.bodyLarge)
                Text(expense.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Text("- ${expense.amount} VND", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun AddExpenseForm(onSave: (amount: Double, category: String, desc: String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }

    val categories = listOf("Food", "Transport", "Accommodation", "Activities", "Shopping")

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Add New Expense", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (VND)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("What was it for?") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Category:", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Simple chips for category selection
            categories.take(3).forEach { cat ->
                FilterChip(
                    selected = (category == cat),
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(amount.toDoubleOrNull() ?: 0.0, category, description) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Text("Save Expense")
        }
    }
}