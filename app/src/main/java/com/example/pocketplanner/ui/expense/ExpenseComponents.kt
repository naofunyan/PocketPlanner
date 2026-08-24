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
        color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
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
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        "TOTAL SPENT",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, // DYNAMIC TEXT
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            currencyFormatter.format(budgetState.totalSpent),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            " / ${currencyFormatter.format(budgetState.totalBudget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // DYNAMIC TEXT
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer, // DYNAMIC BACKGROUND
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Wallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, // DYNAMIC ICON
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
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, // DYNAMIC COLOR
                trackColor = MaterialTheme.colorScheme.surfaceVariant // DYNAMIC BACKGROUND
            )
            Spacer(modifier = Modifier.height(8.dp))
            val percentStr = "${(progress * 100).toInt()}%"
            Text(
                "$percentStr of budget used",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, // DYNAMIC COLOR
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
            color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT (Fixes the hidden text issue)
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

        val textMeasurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) // DYNAMIC TEXT

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // DYNAMIC BORDER
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val chartDiameter = canvasHeight * 0.5f
                        val chartRadius = chartDiameter / 2f
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val topLeft = Offset(center.x - chartRadius, center.y - chartRadius)
                        val chartSize = Size(chartDiameter, chartDiameter)

                        if (totalAmount <= 0.0) {
                            drawArc(
                                color = Color(0xFF555555), // Fallback empty state chart color
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                topLeft = topLeft, size = chartSize,
                                style = Stroke(width = 40f, cap = StrokeCap.Butt)
                            )
                        } else {
                            var startAngle = -90f

                            sortedTotals.forEach { (category, amount) ->
                                if (amount > 0) {
                                    val sweepAngle = ((amount / totalAmount) * 360f).toFloat()

                                    drawArc(
                                        color = getCategoryColor(category),
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = chartSize,
                                        style = Stroke(width = 40f, cap = StrokeCap.Butt)
                                    )

                                    val percentage = (amount / totalAmount) * 100

                                    if (percentage >= 2.0) {
                                        val midAngle = startAngle + (sweepAngle / 2f)
                                        val angleInRad = midAngle * (PI / 180.0)

                                        val lineStartRadius = chartRadius + 20f
                                        val lineStartX = center.x + lineStartRadius * cos(angleInRad).toFloat()
                                        val lineStartY = center.y + lineStartRadius * sin(angleInRad).toFloat()

                                        val lineEndRadius = chartRadius + 60f
                                        val lineEndX = center.x + lineEndRadius * cos(angleInRad).toFloat()
                                        val lineEndY = center.y + lineEndRadius * sin(angleInRad).toFloat()

                                        drawLine(
                                            color = Color.Gray, // Static gray for the line pointer
                                            start = Offset(lineStartX, lineStartY),
                                            end = Offset(lineEndX, lineEndY),
                                            strokeWidth = 2f
                                        )

                                        val percentText = String.format(java.util.Locale.US, "%.1f%%", percentage)
                                        val textLayout = textMeasurer.measure(percentText, labelStyle)

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
                                    color = MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT
                                )
                                Text(
                                    text = currencyFormatter.format(amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // DYNAMIC TEXT
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
            color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface, // DYNAMIC BACKGROUND
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // DYNAMIC BORDER
            ) {
                Column {
                    val recentExpenses = expenses.sortedByDescending { it.date }.take(5)

                    recentExpenses.forEachIndexed { index, expense ->
                        TransactionRowItem(
                            expense = expense,
                            formatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            onDelete = { expenseToDelete -> onDeleteClick(expenseToDelete) }
                        )

                        if (index < recentExpenses.lastIndex || expenses.size > 5) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp) // DYNAMIC DIVIDER
                        }
                    }

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
                                color = MaterialTheme.colorScheme.primary, // DYNAMIC TEXT
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
    onDelete: (com.example.pocketplanner.data.local.entity.ExpenseEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${expense.description}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(expense)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
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
                onLongClick = { showDeleteDialog = true }
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), // DYNAMIC BACKGROUND
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val dateStr = dateFormatter.format(Date(expense.date))
            Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) // DYNAMIC TEXT
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatter.format(expense.amount),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface // DYNAMIC TEXT
        )
    }
}