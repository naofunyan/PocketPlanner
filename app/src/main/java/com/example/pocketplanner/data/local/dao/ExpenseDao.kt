package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    // DELETE FUNCTION:
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Flow automatically updates the UI when a new expense is added
    @Query("SELECT * FROM expenses WHERE tripId = :tripId AND isDeleted = 0 ORDER BY date DESC")
    fun getExpensesForTrip(tripId: String): Flow<List<ExpenseEntity>>

    // Calculates the total spent across all expenses for a trip
    @Query("SELECT SUM(convertedAmountVND) FROM expenses WHERE tripId = :tripId AND isDeleted = 0")
    fun getTotalSpentForTrip(tripId: String): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId AND isSyncedWithCloud = 0")
    suspend fun getUnsyncedExpensesForTrip(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseByIdDirect(expenseId: String): ExpenseEntity?
}