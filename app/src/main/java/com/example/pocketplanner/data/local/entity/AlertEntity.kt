package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val placeName: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)