package com.example.pocketplanner.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import java.text.SimpleDateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetProgressCard(
    budgetState: BudgetState,
    currencyFormatter: NumberFormat
) {
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
                // 1. ADDED weight(1f) to force this column to respect the icon's space
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        "TOTAL SPENT",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Swapped Row for FlowRow so long numbers wrap instead of squishing
                    FlowRow(
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            currencyFormatter.format(budgetState.totalSpent),
                            style = MaterialTheme.typography.titleLarge, // 3. Scaled down from headlineMedium for better fit
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            " / ${currencyFormatter.format(budgetState.totalBudget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // The Icon Surface is now perfectly protected from shrinking
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Wallet,
                        contentDescription = null,
                        tint = Color(0xFF005b9f),
                        modifier = Modifier.padding(12.dp)
                    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySummaryRow(
    expenses: List<com.example.pocketplanner.data.local.entity.ExpenseEntity>,
    currencyFormatter: NumberFormat
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val allCategories = listOf("Food", "Shopping", "Groceries", "Transport", "Lodging", "Gaming", "Books", "Bills")
        val categoryTotals = allCategories.associateWith { 0.0 }.toMutableMap()

        var totalAmount = 0.0
        expenses.forEach { exp ->
            val matchedCat = allCategories.find { it.equals(exp.category, ignoreCase = true) } ?: exp.category
            categoryTotals[matchedCat] = (categoryTotals[matchedCat] ?: 0.0) + exp.amount
            totalAmount += exp.amount
        }

        val sortedTotals = categoryTotals.toList().sortedByDescending { it.second }

        // 1. Prepare the text measurer for the Canvas labels
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray, fontWeight = FontWeight.Bold)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5F5F5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. The Donut Chart (Expanded to give the lines and text room)
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Scale the chart to 50% of the canvas height so labels don't get cut off
                        val chartDiameter = canvasHeight * 0.5f
                        val chartRadius = chartDiameter / 2f
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val topLeft = Offset(center.x - chartRadius, center.y - chartRadius)
                        val chartSize = Size(chartDiameter, chartDiameter)

                        if (totalAmount <= 0.0) {
                            drawArc(
                                color = Color(0xFFEEEEEE),
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                topLeft = topLeft, size = chartSize,
                                style = Stroke(width = 40f, cap = StrokeCap.Butt)
                            )
                        } else {
                            var startAngle = -90f // 12 o'clock

                            sortedTotals.forEach { (category, amount) ->
                                if (amount > 0) {
                                    val sweepAngle = ((amount / totalAmount) * 360f).toFloat()

                                    // A. Draw the colored arc
                                    drawArc(
                                        color = getCategoryColor(category),
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = chartSize,
                                        style = Stroke(width = 40f, cap = StrokeCap.Butt)
                                    )

                                    // B. Draw the line and text label
                                    val percentage = (amount / totalAmount) * 100

                                    // Only draw labels for slices larger than 2% to prevent text overlap
                                    if (percentage >= 2.0) {
                                        val midAngle = startAngle + (sweepAngle / 2f)
                                        val angleInRad = midAngle * (PI / 180.0)

                                        // Start the line just outside the 40f stroke
                                        val lineStartRadius = chartRadius + 20f
                                        val lineStartX = center.x + lineStartRadius * cos(angleInRad).toFloat()
                                        val lineStartY = center.y + lineStartRadius * sin(angleInRad).toFloat()

                                        // End the line further out
                                        val lineEndRadius = chartRadius + 60f
                                        val lineEndX = center.x + lineEndRadius * cos(angleInRad).toFloat()
                                        val lineEndY = center.y + lineEndRadius * sin(angleInRad).toFloat()

                                        drawLine(
                                            color = Color.LightGray,
                                            start = Offset(lineStartX, lineStartY),
                                            end = Offset(lineEndX, lineEndY),
                                            strokeWidth = 2f
                                        )

                                        // Measure and draw the percentage text
                                        val percentText = String.format(java.util.Locale.US, "%.1f%%", percentage)
                                        val textLayout = textMeasurer.measure(percentText, labelStyle)

                                        // Push text to the left or right depending on which half of the circle it's on
                                        val textX = if (cos(angleInRad) >= 0) {
                                            lineEndX + 8f
                                        } else {
                                            lineEndX - textLayout.size.width - 8f
                                        }
                                        val textY = lineEndY - (textLayout.size.height / 2f)

                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = percentText,
                                            style = labelStyle,
                                            topLeft = Offset(textX, textY)
                                        )
                                    }
                                    startAngle += sweepAngle
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. The Color Legend
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sortedTotals.forEach { (cat, amount) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(getCategoryColor(cat), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1E1E)
                                )
                                Text(
                                    text = currencyFormatter.format(amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListWidget(
    expenses: List<com.example.pocketplanner.data.local.entity.ExpenseEntity>,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onViewMoreClick: () -> Unit = {},
    onDeleteClick: (ExpenseEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            "Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions logged yet.", color = Color.Gray)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5F5F5))
            ) {
                Column {
                    // Grab only the 5 most recent expenses
                    val recentExpenses = expenses.sortedByDescending { it.date }.take(5)

                    recentExpenses.forEachIndexed { index, expense ->
                        // 1. Updated this call to pass the delete command!
                        TransactionRowItem(
                            expense = expense,
                            formatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            onDelete = { expenseToDelete -> onDeleteClick(expenseToDelete) }
                        )

                        if (index < recentExpenses.lastIndex || expenses.size > 5) {
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        }
                    }

                    // Only show the button if there are actually more than 5 transactions
                    if (expenses.size > 5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewMoreClick() }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "View more transactions",
                                color = Color(0xFF4285F4), // Standard actionable blue
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    expense: com.example.pocketplanner.data.local.entity.ExpenseEntity,
    formatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onDelete: (com.example.pocketplanner.data.local.entity.ExpenseEntity) -> Unit // 1. Added the delete command!
) {
    // 2. Add the dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 3. Add the confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${expense.description}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense) // Tells the database to delete it!
                }) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val icon = getIconForCategory(expense.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Handle normal tap to view details here later */ },
                onLongClick = { showDeleteDialog = true } // 4. Triggers the dialog!
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val dateStr = dateFormatter.format(Date(expense.date))
            Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatter.format(expense.amount),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )
    }
}

// NOTE: The rest of the helper functions (CategoryCard, TransactionRow, getIconForCategory, AddExpenseForm, BottomNavPill)
// are currently at the bottom of your ExpenseScreen.kt and GlobalWalletScreen.kt files.
// You can leave them there for now, or move them here if you want to clean up those files!