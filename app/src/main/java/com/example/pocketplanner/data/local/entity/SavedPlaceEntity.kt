package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val placeName: String
)