package com.example.pocketplanner.ui.navigation

import kotlinx.serialization.Serializable

// The screens our app has
@Serializable
object LoginRoute

@Serializable
object HomeRoute // Where we show the list of trips

@Serializable
data class DayPlanRoute(val tripId: String, val dayNumber: Int)

@Serializable
data class ItineraryRoute(val tripId: String)

@Serializable
data class ExpenseRoute(val tripId: String)

@Serializable
object ProfileRoute