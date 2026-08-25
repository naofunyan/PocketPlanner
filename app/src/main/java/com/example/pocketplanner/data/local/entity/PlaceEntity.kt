package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val tripId: String, // Which trip this place belongs to
    val dayNumber: Int, // Which day of the trip (1, 2, 3...)
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String, // e.g. "Restaurant", "Attraction"
    val estimatedCost: Double = 0.0,
    val notes: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val photoUrl: String? = null,
    val isVisited: Boolean = false,
    val userRating: Int? = null,
    val isSyncedWithCloud: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)