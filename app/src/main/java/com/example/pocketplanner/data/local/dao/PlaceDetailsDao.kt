package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.PlaceDetailsEntity

@Dao
interface PlaceDetailsDao {
    @Query("SELECT * FROM place_details WHERE placeId = :placeId LIMIT 1")
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceDetails(details: PlaceDetailsEntity)
}
