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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pocketplanner.ui.auth.LoginScreen
import com.example.pocketplanner.ui.auth.SignUpScreen
import com.example.pocketplanner.ui.auth.WelcomeScreen
import com.example.pocketplanner.ui.expense.ExpenseScreen
import com.example.pocketplanner.ui.itinerary.DayPlanScreen
import com.example.pocketplanner.ui.itinerary.ItineraryScreen

import android.annotation.SuppressLint

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // We only want to show the bottom bar if the user is NOT on the login/welcome screens
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route?.contains("LoginRoute") == false && 
                        currentDestination?.route?.contains("WelcomeRoute") == false && 
                        currentDestination?.route?.contains("SignUpRoute") == false &&
                        currentDestination?.route?.contains("SearchRoute") == false &&
                        currentDestination?.route?.contains("CreateTripDetailsRoute") == false &&
                        currentDestination?.route?.contains("ItineraryRoute") == false &&
                        currentDestination?.route?.contains("ExpenseRoute") == false &&
                        currentDestination?.route?.contains("TrackingRoute") == false

    Scaffold(
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
                                icon = Icons.Filled.Home,
                                label = "Home",
                                isSelected = isHome,
                                onClick = {
                                    navController.navigate(HomeRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            val isExplore = currentDestination?.hierarchy?.any { it.route?.contains("ExploreRoute") == true } == true
                            BottomNavTab(
                                icon = Icons.Filled.Explore,
                                label = "Explore",
                                isSelected = isExplore,
                                onClick = {
                                    navController.navigate(ExploreRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                            BottomNavTab(
                                icon = Icons.Filled.Notifications,
                                label = "Alerts",
                                isSelected = false,
                                onClick = { /* TODO */ }
                            )

                            BottomNavTab(
                                icon = Icons.Filled.Settings,
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
                            modifier = Modifier.fillMaxSize().clickable { /* TODO: AI Call */ }
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.AutoAwesome, 
                                contentDescription = "AI Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp) // Made icon slightly larger since text is gone
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
            startDestination = WelcomeRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<WelcomeRoute> {
                WelcomeScreen(
                    onNavigateToSignUp = { navController.navigate(SignUpRoute) },
                    onNavigateToLogin = { navController.navigate(LoginRoute) },
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignUp = { navController.navigate(SignUpRoute) },
                    onLoginSuccess = {
                        // When login succeeds, go to Home and remove Login from the backstack
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<SignUpRoute> {
                SignUpScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(LoginRoute) },
                    onSignUpSuccess = {
                        // When sign up succeeds, go to Home and clear backstack
                        navController.navigate(HomeRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<HomeRoute> {
                // Grab the currently logged-in user from Firebase
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    com.example.pocketplanner.ui.itinerary.TripsScreen(
                        userId = currentUser.uid,
                        onTripClick = { tripId -> navController.navigate(ItineraryRoute(tripId)) },
                        onAddTripClick = { navController.navigate(SearchRoute) }
                    )
                } else {
                    Text("Error: Not logged in!")
                }
            }

            composable<ExploreRoute> {
                com.example.pocketplanner.ui.explore.ExploreScreen(
                    onSearchClick = { navController.navigate(SearchRoute) }
                )
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
                com.example.pocketplanner.ui.itinerary.CreateTripDetailsScreen(
                    destinations = args.destinations,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateTrip = { 
                        // Right now just navigate back to home
                        navController.popBackStack(HomeRoute, inclusive = false)
                    }
                )
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
                    onExpenseClick = { navController.navigate(ExpenseRoute(route.tripId)) },
                    onTrackClick = { navController.navigate(TrackingRoute(route.tripId)) }
                )
            }

            composable<ExpenseRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ExpenseRoute>()
                com.example.pocketplanner.ui.expense.ExpenseScreen(
                    tripId = args.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onPlanClick = { navController.popBackStack() },
                    onTrackClick = { navController.navigate(TrackingRoute(args.tripId)) }
                )
            }

            composable<TrackingRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<TrackingRoute>()
                com.example.pocketplanner.ui.tracking.TrackingScreen(
                    tripId = args.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onPlanClick = { navController.popBackStack() },
                    onExpenseClick = { navController.navigate(ExpenseRoute(args.tripId)) }
                )
            }

            composable<ProfileRoute> {
                // Placeholder for now!
                Text("User Profile Settings")
            }
        }
    }
}

@Composable
fun BottomNavTab(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Center the icon by providing consistent top padding, removing the active dot
        Spacer(modifier = Modifier.height(4.dp))

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
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