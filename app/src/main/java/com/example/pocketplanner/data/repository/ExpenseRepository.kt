package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.ExpenseDao
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import javax.inject.Inject

class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun getExpenses(tripId: String) = expenseDao.getExpensesForTrip(tripId)
    fun getTotalSpent(tripId: String) = expenseDao.getTotalSpentForTrip(tripId)
    suspend fun addExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)
}