package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_details")
data class PlaceDetailsEntity(
    @PrimaryKey val placeId: String, // Foreign key to PlaceEntity.id
    val foursquareId: String? = null,
    val address: String? = null,
    val formattedHours: String? = null,
    val aiDescription: String? = null,
    val aiTip: String? = null,
    val rating: Double? = null,
    val photoUrls: String? = null,
    val price: String? = null,
    val fetchedAt: Long = System.currentTimeMillis()
)
