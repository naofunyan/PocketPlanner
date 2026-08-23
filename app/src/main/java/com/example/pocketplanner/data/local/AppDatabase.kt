package com.example.pocketplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pocketplanner.data.local.dao.AlertDao
import com.example.pocketplanner.data.local.dao.ExpenseDao
import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.SavedPlaceDao
import com.example.pocketplanner.data.local.dao.TicketDao
import com.example.pocketplanner.data.local.dao.TrackingPointDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.AlertEntity
import com.example.pocketplanner.data.local.entity.ExpenseEntity
import com.example.pocketplanner.data.local.entity.PlaceDetailsEntity
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.SavedPlaceEntity
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.data.local.entity.TrackingPointEntity
import com.example.pocketplanner.data.local.entity.TripEntity

@Database(
    entities = [
        TripEntity::class, PlaceEntity::class, ExpenseEntity::class, TrackingPointEntity::class,
        AlertEntity::class, PlaceDetailsEntity::class, TicketEntity::class,
        SavedPlaceEntity::class // <-- ADDED THIS
    ],
    version = 17, // <-- BUMPED TO 16
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room will automatically implement these for us
    abstract fun tripDao(): TripDao
    abstract fun placeDao(): PlaceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun trackingPointDao(): TrackingPointDao
    abstract fun alertDao(): AlertDao
    abstract fun placeDetailsDao(): com.example.pocketplanner.data.local.dao.PlaceDetailsDao
    abstract fun ticketDao(): TicketDao

    abstract fun savedPlaceDao(): SavedPlaceDao // <-- ADDED THIS
}