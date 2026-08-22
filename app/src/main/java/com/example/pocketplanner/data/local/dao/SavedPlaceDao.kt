package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT placeName FROM saved_places")
    fun getAllSavedPlaces(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE placeName = :placeName")
    suspend fun deleteSavedPlace(placeName: String)
}