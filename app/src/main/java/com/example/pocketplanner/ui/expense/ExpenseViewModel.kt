package com.example.pocketplanner.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.repository.ExpenseRepository
import com.example.pocketplanner.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    fun getExpenses(tripId: String): Flow<List<ExpenseEntity>> = expenseRepository.getExpenses(tripId)

    // Combine Trip Budget and Total Spent into one state for the UI
    fun getBudgetOverview(tripId: String): Flow<BudgetState> {
        return combine(
            tripRepository.getTrip(tripId),
            expenseRepository.getTotalSpent(tripId)
        ) { trip, totalSpent ->
            BudgetState(
                totalBudget = trip?.budget ?: 0.0,
                totalSpent = totalSpent ?: 0.0,
                remaining = (trip?.budget ?: 0.0) - (totalSpent ?: 0.0)
            )
        }
    }

    fun addExpense(tripId: String, category: String, amount: Double, description: String) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                category = category,
                amount = amount,
                convertedAmountVND = amount, // Assuming input is VND for now
                description = description,
                date = System.currentTimeMillis()
            )
            expenseRepository.addExpense(expense)
        }
    }
}

data class BudgetState(val totalBudget: Double, val totalSpent: Double, val remaining: Double)