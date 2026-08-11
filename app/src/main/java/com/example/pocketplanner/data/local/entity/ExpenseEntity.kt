package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,          // Links this expense to a specific trip
    val category: String,        // e.g., "Food", "Transport", "Shopping"
    val amount: Double,
    val currency: String = "VND",
    val convertedAmountVND: Double, // We sum this column to calculate total spent
    val description: String,
    val date: Long,
    val isSyncedWithCloud: Boolean = false // Ready for FirestoreSyncManager later
)