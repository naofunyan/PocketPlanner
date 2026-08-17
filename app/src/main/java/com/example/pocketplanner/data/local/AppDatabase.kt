package com.example.pocketplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pocketplanner.data.local.dao.AlertDao
import com.example.pocketplanner.data.local.dao.ExpenseDao
import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TrackingPointDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.AlertEntity
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.local.entity.TrackingPointEntity

// If we add more tables later, we just add them to the entities array and bump the version
@Database(
    entities = [TripEntity::class, PlaceEntity::class, ExpenseEntity::class, TrackingPointEntity::class, AlertEntity::class],
    version = 8, // Bump version to clear DB and apply tracker fields to TripEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room will automatically implement these for us
    abstract fun tripDao(): TripDao
    abstract fun placeDao(): PlaceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun trackingPointDao(): TrackingPointDao
    abstract fun alertDao(): AlertDao
}