package com.example.pocketplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity

// If we add more tables later, we just add them to the entities array and bump the version
@Database(
    entities = [TripEntity::class, PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room will automatically implement these for us
    abstract fun tripDao(): TripDao
    abstract fun placeDao(): PlaceDao
}