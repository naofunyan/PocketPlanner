package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @androidx.room.Update
    suspend fun updatePlace(place: PlaceEntity)

    @androidx.room.Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("SELECT * FROM places WHERE tripId = :tripId AND dayNumber = :dayNumber ORDER BY name ASC")
    fun getPlacesForDay(tripId: String, dayNumber: Int): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE tripId = :tripId AND dayNumber = :dayNumber")
    suspend fun getPlacesForDaySync(tripId: String, dayNumber: Int): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY dayNumber ASC, name ASC")
    fun getAllPlacesForTrip(tripId: String): Flow<List<PlaceEntity>>
    
    @Query("DELETE FROM places WHERE tripId = :tripId AND dayNumber > :maxDayNumber")
    suspend fun deletePlacesForDaysGreaterThan(tripId: String, maxDayNumber: Int)
}