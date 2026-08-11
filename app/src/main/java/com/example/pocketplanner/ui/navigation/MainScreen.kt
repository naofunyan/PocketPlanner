package com.example.pocketplanner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pocketplanner.ui.auth.LoginScreen
import com.example.pocketplanner.ui.itinerary.DayPlanScreen
import com.example.pocketplanner.ui.itinerary.ItineraryScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // We only want to show the bottom bar if the user is NOT on the login screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route?.contains("LoginRoute") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Home Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Trips") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("HomeRoute") == true } == true,
                        onClick = {
                            navController.navigate(HomeRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // Profile Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("ProfileRoute") == true } == true,
                        onClick = {
                            navController.navigate(ProfileRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // This NavHost is where the actual screens are swapped in and out
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        // When login succeeds, go to Home and remove Login from the backstack
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
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
                        onTripClick = { tripId -> navController.navigate(ItineraryRoute(tripId)) }
                    )
                } else {
                    Text("Error: Not logged in!")
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
                val itinerary = backStackEntry.toRoute<ItineraryRoute>()
                ItineraryScreen(
                    tripId = itinerary.tripId,
                    onNavigateBack = { navController.popBackStack() },
                    onDayClick = { dayNumber ->
                        navController.navigate(DayPlanRoute(itinerary.tripId, dayNumber))
                    }
                )
            }

            composable<ProfileRoute> {
                // Placeholder for now!
                Text("User Profile Settings")
            }
        }
    }
}