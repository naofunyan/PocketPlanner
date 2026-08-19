package com.example.pocketplanner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pocketplanner.ui.components.OfflineBannerWrapper
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.ui.auth.WelcomeScreen

import com.example.pocketplanner.ui.expense.ExpenseScreen
import com.example.pocketplanner.ui.itinerary.DayPlanScreen
import com.example.pocketplanner.ui.itinerary.ItineraryScreen
import com.example.pocketplanner.ui.components.OfflineBannerWrapper

import android.annotation.SuppressLint

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // We only want to show the bottom bar if the user is NOT on the login/welcome screens
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // We only show the bottom bar if the current route is NOT AuthRoute or WelcomeRoute
    val showBottomBar = currentDestination?.route?.contains("AuthRoute") == false && 
                        currentDestination?.route?.contains("WelcomeRoute") == false &&
                        currentDestination?.route?.contains("SearchRoute") == false &&
                        currentDestination?.route?.contains("CreateTripDetailsRoute") == false &&
                        currentDestination?.route?.contains("EditTripDetailsRoute") == false &&
                        currentDestination?.route?.contains("ItineraryRoute") == false &&
                        currentDestination?.route?.contains("ExpenseRoute") == false &&
                        currentDestination?.route?.contains("TrackingRoute") == false &&
                        currentDestination?.route?.contains("ChatRoute") == false &&
                        currentDestination?.route?.contains("ExploreDetailsRoute") == false

    OfflineBannerWrapper {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp), // Floating padding
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Main Navigation Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isHome = currentDestination?.hierarchy?.any { it.route?.contains("HomeRoute") == true } == true
                            BottomNavTab(
                                iconResId = com.example.pocketplanner.R.drawable.navhome,
                                label = "Home",
                                isSelected = isHome,
                                onClick = {
                                    navController.navigate(HomeRoute) {
                                        popUpTo<HomeRoute> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            val isExplore = currentDestination?.hierarchy?.any { it.route?.contains("ExploreRoute") == true } == true
                            BottomNavTab(
                                iconResId = com.example.pocketplanner.R.drawable.navexplore,
                                label = "Explore",
                                isSelected = isExplore,
                                onClick = {
                                    navController.navigate(ExploreRoute) {
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            val isExpense = currentDestination?.hierarchy?.any { it.route?.contains("ExpenseRoute") == true } == true
                            BottomNavTab(
                                iconResId = com.example.pocketplanner.R.drawable.navwallet,
                                label = "Wallet",
                                isSelected = isExpense,
                                onClick = {
                                    navController.navigate(ExpenseRoute("")) { // Navigate to global expense for now
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            BottomNavTab(
                                iconResId = com.example.pocketplanner.R.drawable.navsettings,
                                label = "Settings",
                                isSelected = false,
                                onClick = { /* TODO */ }
                            )
                        }
                    }
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { navController.navigate(ChatRoute) } // <-- ADD THIS
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.pocketplanner.R.drawable.navaction),
                                    contentDescription = "AI Call",
                                    modifier = Modifier.size(32.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                } // End Row
                } // End Box
            }
        }
    ) { innerPadding ->
        // This NavHost is where the actual screens are swapped in and out
        // We DO NOT pad the NavHost by innerPadding so it draws full-screen, BEHIND the floating bottom nav
        NavHost(
            navController = navController,
            startDestination = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) HomeRoute else WelcomeRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { fadeIn(animationSpec = tween(400)) },
            popExitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            composable<WelcomeRoute>(
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) }
            ) {
                WelcomeScreen(
                    onNavigateToAuth = { navController.navigate(AuthRoute(initialIsLogin = true)) }
                )
            }

            composable<AuthRoute>(
                enterTransition = { 
                    slideIntoContainer(
                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(400)
                    )
                },
                popExitTransition = { 
                    slideOutOfContainer(
                        towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(400)
                    )
                }
            ) { backStackEntry ->
                val authRoute = backStackEntry.toRoute<AuthRoute>()
                com.example.pocketplanner.ui.auth.AuthScreen(
                    initialIsLoginMode = authRoute.initialIsLogin,
                    onAuthSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    },
                    onGuestLogin = {
                        navController.navigate(HomeRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<ImportTripRoute>(
                deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "pocketplanner://trip/{tripId}" })
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<ImportTripRoute>()
                com.example.pocketplanner.ui.itinerary.ImportTripScreen(
                    tripId = args.tripId,
                    onImportComplete = {
                        navController.navigate(HomeRoute) {
                            popUpTo(0) { inclusive = true } // Clear stack and go home
                        }
                    },
                    onCancel = {
                        navController.navigate(HomeRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<HomeRoute> {
                // Grab the currently logged-in user from Firebase
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val uid = currentUser?.uid ?: "guest"
                
                com.example.pocketplanner.ui.itinerary.TripsScreen(
                    userId = uid,
                    onTripClick = { tripId -> navController.navigate(ItineraryRoute(tripId)) },
                    onEditTripClick = { tripId -> navController.navigate(EditTripDetailsRoute(tripId)) },
                    onAddTripClick = { navController.navigate(SearchRoute) }
                )
            }

            composable<ExploreRoute> {
                com.example.pocketplanner.ui.explore.ExploreScreen(
                    onSearchClick = { navController.navigate(SearchRoute) },
                    onNavigateToDetails = { destId -> navController.navigate(ExploreDetailsRoute(destId)) }
                )
            }

            composable<ExploreDetailsRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ExploreDetailsRoute>()
                com.example.pocketplanner.ui.explore.ExploreDetailsScreen(
                    destinationId = args.destinationId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddToTrip = {
                        // For now, bounce the user to the SearchScreen so they can start creating a trip!
                        navController.navigate(SearchRoute)
                    }
                )
            }

            composable<AlertsRoute> {
                com.example.pocketplanner.ui.alerts.AlertsScreen()
            }

            composable<SearchRoute> {
                com.example.pocketplanner.ui.search.SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetails = { dests ->
                        navController.navigate(CreateTripDetailsRoute(destinations = dests.joinToString(", ")))
                    }
                )
            }

            composable<CreateTripDetailsRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<CreateTripDetailsRoute>()
                val itineraryViewModel: com.example.pocketplanner.ui.itinerary.ItineraryViewModel = hiltViewModel()
                val isGenerating by itineraryViewModel.isGenerating.collectAsState()
                val coverPhotoUrl by itineraryViewModel.coverPhotoUrl.collectAsState()
                val trips by itineraryViewModel.trips.collectAsState()
                val exchangeRates by itineraryViewModel.exchangeRates.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current

                androidx.compose.runtime.LaunchedEffect(args.destinations) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val userId = currentUser?.uid ?: "test_user_id"
                    itineraryViewModel.loadTrips(userId)
                    itineraryViewModel.fetchExchangeRates()
                    // Use the first destination to find a relevant photo
                    val firstDest = args.destinations.split(",").firstOrNull()?.trim() ?: args.destinations
                    itineraryViewModel.fetchInitialCoverPhoto(firstDest)
                }

                com.example.pocketplanner.ui.itinerary.CreateTripDetailsScreen(
                    destinations = args.destinations,
                    isGenerating = isGenerating,
                    coverPhotoUrl = coverPhotoUrl,
                    existingTrips = trips,
                    exchangeRates = exchangeRates,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateTrip = { days, name, start, end, customPhotoUrl, budgetAmt, budgetCurr, isTrackerEnabled, trackingMode ->
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        val userId = currentUser?.uid ?: "test_user_id"
                        itineraryViewModel.generateTripWithAI(
                            userId = userId, 
                            destination = args.destinations, 
                            days = days, 
                            name = name,
                            startDate = start,
                            endDate = end,
                            customPhotoUrl = customPhotoUrl,
                            budgetAmount = budgetAmt,
                            budgetCurrency = budgetCurr,
                            isTrackerEnabled = isTrackerEnabled,
                            trackingMode = trackingMode,
                            onSuccess = { 
                                navController.popBackStack(HomeRoute, inclusive = false)
                            },
                            onError = { error ->
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }

            composable<EditTripDetailsRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<EditTripDetailsRoute>()
                val itineraryViewModel: com.example.pocketplanner.ui.itinerary.ItineraryViewModel = hiltViewModel()
                val isGenerating by itineraryViewModel.isGenerating.collectAsState()
                val trips by itineraryViewModel.trips.collectAsState()
                val exchangeRates by itineraryViewModel.exchangeRates.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                
                val existingTrip = trips.find { it.id == args.tripId }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val userId = currentUser?.uid ?: "test_user_id"
                    if (trips.isEmpty()) {
                        itineraryViewModel.loadTrips(userId)
                    }
                    itineraryViewModel.fetchExchangeRates()
                }

                if (existingTrip != null) {
                    com.example.pocketplanner.ui.itinerary.CreateTripDetailsScreen(
                        destinations = existingTrip.destination, // Pre-fill but hide in UI
                        isGenerating = isGenerating,
                        coverPhotoUrl = existingTrip.photoUrl,
                        existingTrips = trips,
                        exchangeRates = exchangeRates,
                        editTripId = existingTrip.id,
                        existingTripData = existingTrip,
                        onNavigateBack = { navController.popBackStack() },
                        onCreateTrip = { _, name, start, end, photoUrl, budgetAmt, budgetCurr, isTrackerEnabled, trackingMode ->
                            itineraryViewModel.updateTripSettings(
                                tripId = existingTrip.id,
                                newName = name,
                                newStart = start,
                                newEnd = end,
                                newBudgetAmount = budgetAmt,
                                newBudgetCurrency = budgetCurr,
                                isTrackerEnabled = isTrackerEnabled,
                                trackingMode = trackingMode,
                                newPhotoUrl = photoUrl,
                                onSuccess = {
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                } else {
                    // Show loading or fallback
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }

            composable<DayPlanRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<DayPlanRoute>()
                DayPlanScreen(
                    tripId = route.tripId,
                    dayNumber = route.dayNumber,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ItineraryRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItineraryRoute>()
                ItineraryScreen(
                    tripId = route.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onDayClick = { dayNumber ->
                        navController.navigate(DayPlanRoute(route.tripId, dayNumber))
                    },
                    onExpenseClick = { 
                        navController.navigate(ExpenseRoute(route.tripId)) {
                            popUpTo(route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    },
                    onTrackClick = { 
                        navController.navigate(TrackingRoute(route.tripId)) {
                            popUpTo(route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
            }

            composable<ExpenseRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ExpenseRoute>()
                com.example.pocketplanner.ui.expense.ExpenseScreen(
                    tripId = args.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onPlanClick = { 
                        navController.navigate(ItineraryRoute(args.tripId)) {
                            popUpTo(ItineraryRoute(args.tripId)) { inclusive = false }
                            launchSingleTop = true
                        } 
                    },
                    onTrackClick = { 
                        navController.navigate(TrackingRoute(args.tripId)) {
                            popUpTo(ItineraryRoute(args.tripId)) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
            }

            composable<TrackingRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<TrackingRoute>()
                com.example.pocketplanner.ui.tracking.TrackingScreen(
                    tripId = args.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onPlanClick = { 
                        navController.navigate(ItineraryRoute(args.tripId)) {
                            popUpTo(ItineraryRoute(args.tripId)) { inclusive = false }
                            launchSingleTop = true
                        } 
                    },
                    onExpenseClick = { 
                        navController.navigate(ExpenseRoute(args.tripId)) {
                            popUpTo(ItineraryRoute(args.tripId)) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
            }

            composable<ChatRoute> {
                com.example.pocketplanner.ui.chat.AiHubScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ProfileRoute> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "User Profile Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Signed in as: ${currentUser?.email ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            // Navigate to AuthRoute and clear backstack
                            navController.navigate(WelcomeRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}
}

@Composable
fun BottomNavTab(iconResId: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Center the icon by providing consistent top padding, removing the active dot
        Spacer(modifier = Modifier.height(4.dp))

        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = iconResId),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            maxLines = 1
        )
    }
}