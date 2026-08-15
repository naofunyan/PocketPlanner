package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String, // We'll use UUIDs or Firestore IDs
    val userId: String, // To support multiple users/guests
    val name: String = "",
    val destination: String,
    val startDate: Long, // Stored as Unix timestamp
    val endDate: Long,
    val budget: Double,
    val currency: String = "VND",
    val status: String = "UPCOMING", // UPCOMING, ACTIVE, PAST
    val photoUrl: String? = null,
    val isSyncedWithCloud: Boolean = false
)