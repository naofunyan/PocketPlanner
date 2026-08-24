package com.example.pocketplanner.ui.navigation

import kotlinx.serialization.Serializable

// The screens our app has
@Serializable
object WelcomeRoute

@Serializable
data class AuthRoute(val initialIsLogin: Boolean = true)

@Serializable
object HomeRoute // Where we show the list of trips

@Serializable
object ExploreRoute

@Serializable
object GlobalWalletRoute

@Serializable
data class TransactionHistoryRoute(val tripId: String)

@Serializable
object SearchRoute

@Serializable
data class CreateTripDetailsRoute(val destinations: String)

@Serializable
data class EditTripDetailsRoute(val tripId: String)

@Serializable
data class DayPlanRoute(val tripId: String, val dayNumber: Int)

@Serializable
data class ItineraryRoute(val tripId: String)

@Serializable
data class ExpenseRoute(val tripId: String)

@Serializable
data class ImportTripRoute(val tripId: String)

@Serializable
object ChatRoute

@Serializable
data class ExploreDetailsRoute(val destinationId: String)

@Serializable
object AlertsRoute

@Serializable
data class AllPlacesRoute(val destinationId: String)

@Serializable
data class PlaceDetailsRoute(val placeName: String)

@Serializable
object SettingsRoute

@Serializable
object FallDetectionSettingsRoute

@Serializable
object EmergencySharingRoute

@Serializable
object MedicalInfoRoute

@Serializable
object ProfileRoute