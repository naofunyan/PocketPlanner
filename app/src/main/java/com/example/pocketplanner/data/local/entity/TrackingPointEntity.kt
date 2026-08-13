package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracking_points")
data class TrackingPointEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val accuracy: Float
)