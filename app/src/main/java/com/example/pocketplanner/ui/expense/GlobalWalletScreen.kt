package com.example.pocketplanner.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalWalletScreen(
    onNavigateToHistory: (String) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
    ticketViewModel: TicketViewModel = hiltViewModel()
) {
    // 1. Initialize ViewModel for Global Mode
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        viewModel.initialize("", userId) // Pass empty string to trigger Global Mode
        ticketViewModel.loadTrips(userId)
    }

    // 2. State Observation
    val allTrips by viewModel.allTrips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val trip by viewModel.currentTrip.collectAsState(initial = null)

    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgetState by viewModel.budgetOverview.collectAsState(initial = BudgetState(0.0, 0.0, 0.0))
    val exchangeRates by viewModel.exchangeRates.collectAsState(initial = emptyMap())

    // Ticket state
    val ticketTrips by ticketViewModel.allTrips.collectAsState()
    val filteredTickets by ticketViewModel.filteredTickets.collectAsState(initial = emptyList())
    val ticketFilter by ticketViewModel.selectedFilter.collectAsState()
    val ticketSearchQuery by ticketViewModel.searchQuery.collectAsState()

    // UI States
    val pagerState = rememberPagerState(pageCount = { 2 }) // We have 2 tabs!
    val selectedTabIndex = pagerState.currentPage // Reads the swipe position automatically
    val coroutineScope = rememberCoroutineScope() // Needed to animate button taps

    var showTripSelector by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    var showAddTicketSheet by remember { mutableStateOf(false) }
    var viewingTicket by remember { mutableStateOf<com.example.pocketplanner.data.local.entity.TicketEntity?>(null) }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        floatingActionButton = {
            // Show FAB for Expense tab (when trip selected) or Ticket tab (always)
            if ((selectedTripId != null && selectedTabIndex == 0) || selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTabIndex == 0) showAddSheet = true
                        else showAddTicketSheet = true
                    },
                    containerColor = Color(0xFF005b9f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 92.dp) // Lifted to clear main navbar
                ) {
                    Icon(Icons.Filled.Add, contentDescription = if (selectedTabIndex == 0) "Add Expense" else "Add Ticket")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding() + 24.dp)
        ) {
            // --- HEADER: CONTEXT-AWARE ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (selectedTabIndex == 0) {
                    // EXPENSE TAB: Trip selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = allTrips.isNotEmpty()) { showTripSelector = true }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = trip?.let {
                                if (it.name.isNotBlank()) it.name else "Trip to ${it.destination}"
                            } ?: if (allTrips.isEmpty()) "No Trips Available" else "Loading...",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                        if (allTrips.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Trip", tint = Color.Gray)
                        }
                    }

                    if (trip != null) {
                        val startStr = dateFormatter.format(Date(trip!!.startDate))
                        val endStr = dateFormatter.format(Date(trip!!.endDate))
                        val days = ((trip!!.endDate - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                        Text(
                            text = "$startStr - $endStr • $days Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    // TICKET TAB: Static global header
                    Text(
                        text = "My Tickets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SEGMENTED CONTROL ---
            WalletSegmentedControl(
                selectedIndex = selectedTabIndex, // Automatically matches the swipe position
                onIndexSelected = { index ->
                    // Animate to the selected page when the button is tapped!
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB CONTENT ---
            if (allTrips.isEmpty()) {
                EmptyWalletState()
            } else {
                // The new Swipeable Pager!
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        ExpenseTabContent(
                            budgetState = budgetState,
                            expenses = expenses,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            onViewMoreClick = {
                                selectedTripId?.let { id -> onNavigateToHistory(id) }
                            },
                            onDeleteClick = { expense -> viewModel.deleteExpense(expense) }
                        )
                    } else {
                        TicketTabContent(
                            tickets = filteredTickets,
                            allTrips = ticketTrips,
                            selectedFilter = ticketFilter,
                            onFilterSelected = { filter -> ticketViewModel.setFilter(filter) },
                            onTicketClick = { ticket -> viewingTicket = ticket },
                            onDeleteTicket = { ticket -> ticketViewModel.deleteTicket(ticket) },
                            searchQuery = ticketSearchQuery,
                            onSearchQueryChanged = { ticketViewModel.setSearchQuery(it) }
                        )
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEETS ---
    if (showTripSelector) {
        ModalBottomSheet(onDismissRequest = { showTripSelector = false }, containerColor = Color.White) {
            LazyColumn(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                item {
                    Text("Select a Trip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(allTrips) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectTrip(t.id)
                                showTripSelector = false
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (t.name.isNotBlank()) t.name else "Trip to ${t.destination}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (t.id == selectedTripId) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color(0xFF005b9f))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight(), // 1. Force it to stretch all the way to the top!
            dragHandle = null // 2. Hides the default grey drag bar since you have a custom back arrow
        ) {
            AddExpenseForm(
                exchangeRates = exchangeRates,
                onDismiss = { showAddSheet = false },
                onSave = { amount, category, desc, date -> viewModel.addExpense(category, amount, desc, date)
                    showAddSheet = false
                }
            )
        }
    }

    // --- TICKET BOTTOM SHEET ---
    if (showAddTicketSheet) {
        val ticketSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddTicketSheet = false },
            sheetState = ticketSheetState,
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight(),
            dragHandle = null
        ) {
            AddTicketForm(
                allTrips = ticketTrips,
                viewModel = ticketViewModel,
                onDismiss = { showAddTicketSheet = false },
                onSave = { showAddTicketSheet = false }
            )
        }
    }

    // --- TICKET IMAGE VIEWER ---
    viewingTicket?.let { ticket ->
        TicketImageViewer(
            ticket = ticket,
            viewModel = ticketViewModel,
            onDismiss = { viewingTicket = null }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun WalletSegmentedControl(selectedIndex: Int, onIndexSelected: (Int) -> Unit) {
    Surface(
        color = Color.Gray.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
            WalletTabItem("Expense", selectedIndex == 0, { onIndexSelected(0) }, Modifier.weight(1f))
            WalletTabItem("Ticket", selectedIndex == 1, { onIndexSelected(1) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WalletTabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF005b9f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.DarkGray,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ExpenseTabContent(
    budgetState: BudgetState,
    expenses: List<com.example.pocketplanner.data.local.entity.ExpenseEntity>,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    onViewMoreClick: () -> Unit,
    onDeleteClick: (com.example.pocketplanner.data.local.entity.ExpenseEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            BudgetProgressCard(budgetState = budgetState, currencyFormatter = currencyFormatter)
            Spacer(modifier = Modifier.height(32.dp))
            CategorySummaryRow(expenses = expenses, currencyFormatter = currencyFormatter)
            Spacer(modifier = Modifier.height(32.dp))

            // Replaced the old scattered list with our new unified widget!
            TransactionListWidget(
                expenses = expenses,
                currencyFormatter = currencyFormatter,
                dateFormatter = dateFormatter,
                onViewMoreClick = onViewMoreClick,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

@Composable
fun EmptyWalletState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Wallet, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Create a trip to start tracking expenses.", color = Color.Gray)
    }
}