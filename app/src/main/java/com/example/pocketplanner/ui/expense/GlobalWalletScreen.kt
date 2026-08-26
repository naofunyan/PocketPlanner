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
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalWalletScreen(
    onNavigateToHistory: (String) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
    ticketViewModel: TicketViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        viewModel.initialize("", userId)
        ticketViewModel.loadTrips(userId)
    }

    val allTrips by viewModel.allTrips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val trip by viewModel.currentTrip.collectAsState(initial = null)

    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val budgetState by viewModel.budgetOverview.collectAsState(initial = BudgetState(0.0, 0.0, 0.0))
    val exchangeRates by viewModel.exchangeRates.collectAsState(initial = emptyMap())

    val ticketTrips by ticketViewModel.allTrips.collectAsState()
    val filteredTickets by ticketViewModel.filteredTickets.collectAsState(initial = emptyList())
    val ticketFilter by ticketViewModel.selectedFilter.collectAsState()
    val ticketSearchQuery by ticketViewModel.searchQuery.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val selectedTabIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()

    var showTripSelector by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showAddSheet by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    var showAddTicketSheet by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var viewingTicket by remember { mutableStateOf<com.example.pocketplanner.data.local.entity.TicketEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if ((selectedTripId != null && selectedTabIndex == 0) || selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTabIndex == 0) showAddSheet = true
                        else showAddTicketSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary, // <-- DYNAMIC
                    contentColor = MaterialTheme.colorScheme.onPrimary, // <-- DYNAMIC
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 92.dp)
                ) {
                    val fabCd = if (selectedTabIndex == 0) stringResource(R.string.expense_add_fab_cd) else stringResource(R.string.wallet_add_ticket_cd)
                    Icon(Icons.Filled.Add, contentDescription = fabCd)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding() + 24.dp)
        ) {
            // --- HEADER ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (selectedTabIndex == 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = allTrips.isNotEmpty()) { showTripSelector = true }
                            .padding(vertical = 8.dp)
                    ) {
                        val tripTo = stringResource(R.string.expense_trip_to, trip?.destination ?: "")
                        val noTrips = stringResource(R.string.wallet_no_trips)
                        val selectTrip = stringResource(R.string.wallet_select_trip)
                        Text(
                            text = trip?.let {
                                if (it.name.isNotBlank()) it.name else tripTo
                            } ?: if (allTrips.isEmpty()) noTrips else selectTrip,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground, // <-- DYNAMIC
                        )
                        if (allTrips.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.wallet_select_trip_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) // <-- DYNAMIC
                        }
                    }

                    if (trip != null) {
                        val startStr = dateFormatter.format(Date(trip!!.startDate))
                        val ongoing = stringResource(R.string.expense_ongoing)
                        val endStr = if (trip!!.isOpenEnded) ongoing else dateFormatter.format(Date(trip!!.endDate))
                        val days = if (trip!!.isOpenEnded) ((System.currentTimeMillis() - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1 else ((trip!!.endDate - trip!!.startDate) / 86400000L).toInt().coerceAtLeast(0) + 1
                        Text(
                            text = stringResource(R.string.expense_duration_format, startStr, endStr, days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // <-- DYNAMIC
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.wallet_my_tickets),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground, // <-- DYNAMIC
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SEGMENTED CONTROL ---
            WalletSegmentedControl(
                selectedIndex = selectedTabIndex,
                onIndexSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- TAB CONTENT ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    if (allTrips.isEmpty() || selectedTripId == null) {
                        EmptyWalletState(hasTrips = allTrips.isNotEmpty())
                    } else {
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
                    }
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

    // --- BOTTOM SHEETS ---
    if (showTripSelector) {
        val sortedTrips = remember(allTrips) {
            allTrips.sortedByDescending { it.startDate }
        }
        ModalBottomSheet(
            onDismissRequest = { showTripSelector = false }, 
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(stringResource(R.string.wallet_select_trip), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(sortedTrips) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectTrip(t.id)
                                showTripSelector = false
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (t.name.isNotBlank()) t.name else stringResource(R.string.expense_trip_to, t.destination),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (t.id == selectedTripId) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.wallet_trip_selected_cd), tint = MaterialTheme.colorScheme.primary)
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
            containerColor = MaterialTheme.colorScheme.surface, // <-- DYNAMIC
            modifier = Modifier.fillMaxHeight(),
            dragHandle = null
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AddExpenseForm(
                exchangeRates = exchangeRates,
                onDismiss = { showAddSheet = false },
                onSave = { amount, category, desc, date -> 
                    viewModel.addExpense(category, amount, desc, date)
                    showAddSheet = false
                },
                onScanReceipt = { uri -> viewModel.scanReceipt(uri, context) }
            )
        }
    }

    if (showAddTicketSheet) {
        val ticketSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddTicketSheet = false },
            sheetState = ticketSheetState,
            containerColor = MaterialTheme.colorScheme.surface, // <-- DYNAMIC
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
        color = MaterialTheme.colorScheme.surfaceVariant, // <-- FIXED: Was incorrectly onSurface
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
            WalletTabItem(stringResource(R.string.expense_nav_expense), selectedIndex == 0, { onIndexSelected(0) }, Modifier.weight(1f))
            WalletTabItem(stringResource(R.string.wallet_nav_ticket), selectedIndex == 1, { onIndexSelected(1) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WalletTabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, // <-- DYNAMIC
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, // <-- DYNAMIC
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
fun EmptyWalletState(hasTrips: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Wallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasTrips) stringResource(R.string.wallet_empty_state_has_trips) else stringResource(R.string.wallet_empty_state_no_trips), 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}