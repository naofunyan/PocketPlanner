package com.example.pocketplanner.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.ExpenseRepository
import com.example.pocketplanner.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    private val _isGlobalMode = MutableStateFlow(false)
    val isGlobalMode: StateFlow<Boolean> = _isGlobalMode.asStateFlow()

    private val _allTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val allTrips: StateFlow<List<TripEntity>> = _allTrips.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    init {
        fetchExchangeRates()
    }

    private fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Free, no-key CDN API for daily exchange rates
                val response = URL("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json").readText()
                val json = JSONObject(response)
                val usdRates = json.getJSONObject("usd")

                // Build a map of the rates we care about
                val ratesMap = mutableMapOf<String, Double>()
                ratesMap["VND"] = usdRates.getDouble("vnd")
                ratesMap["USD"] = 1.0
                // You can add "EUR" or others here later if needed!

                _exchangeRates.value = ratesMap
            } catch (e: Exception) {
                e.printStackTrace()
                // If offline, fallback to a standard rate so the app doesn't break
                _exchangeRates.value = mapOf("VND" to 25000.0, "USD" to 1.0)
            }
        }
    }

    fun initialize(initialTripId: String, userId: String) {
        if (initialTripId.isNotEmpty()) {
            // TRIP MODE: Lock to the specific trip passed in
            _isGlobalMode.value = false
            _selectedTripId.value = initialTripId
        } else {
            // GLOBAL MODE: Fetch all trips and apply default selection logic
            _isGlobalMode.value = true
            viewModelScope.launch {
                tripRepository.getAllTrips(userId).collect { trips ->
                    _allTrips.value = trips

                    // Only auto-select if the user hasn't manually picked one yet
                    if (_selectedTripId.value == null && trips.isNotEmpty()) {
                        val now = System.currentTimeMillis()

                        // 1. Currently active trip
                        val activeTrip = trips.firstOrNull { it.startDate <= now && it.endDate >= now }
                        // 2. Closest upcoming trip
                        val upcomingTrip = trips.filter { it.startDate > now }.minByOrNull { it.startDate }
                        // 3. Most recent past trip
                        val pastTrip = trips.filter { it.endDate < now }.maxByOrNull { it.endDate }

                        _selectedTripId.value = (activeTrip ?: upcomingTrip ?: pastTrip ?: trips.firstOrNull())?.id
                    }
                }
            }
        }
    }

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    // Reactively swap streams when selectedTripId changes
    val currentTrip: Flow<TripEntity?> = _selectedTripId.flatMapLatest { id ->
        if (id == null) flowOf(null) else tripRepository.getTrip(id)
    }

    val expenses: Flow<List<ExpenseEntity>> = _selectedTripId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else expenseRepository.getExpenses(id)
    }

    val budgetOverview: Flow<BudgetState> = _selectedTripId.flatMapLatest { id ->
        if (id == null) {
            flowOf(BudgetState(0.0, 0.0, 0.0))
        } else {
            combine(
                tripRepository.getTrip(id),
                expenseRepository.getTotalSpent(id)
            ) { trip, totalSpent ->
                BudgetState(
                    totalBudget = trip?.budget ?: 0.0,
                    totalSpent = totalSpent ?: 0.0,
                    remaining = (trip?.budget ?: 0.0) - (totalSpent ?: 0.0)
                )
            }
        }
    }

    // Add "date: Long = System.currentTimeMillis()" to the parameters
    fun addExpense(category: String, amount: Double, description: String, date: Long = System.currentTimeMillis()) {
        val tripId = _selectedTripId.value ?: return
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                category = category,
                amount = amount,
                convertedAmountVND = amount,
                description = description,
                date = date // <-- Use the passed date here!
            )
            expenseRepository.addExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            // Make sure your ExpenseRepository has a deleteExpense function!
            expenseRepository.deleteExpense(expense)
        }
    }
}

data class BudgetState(val totalBudget: Double, val totalSpent: Double, val remaining: Double)