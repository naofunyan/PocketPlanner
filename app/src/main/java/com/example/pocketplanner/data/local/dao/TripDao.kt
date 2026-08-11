package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    // Flow automatically updates the UI when the database changes!
    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startDate ASC")
    fun getAllTripsForUser(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripFlow(tripId: String): Flow<TripEntity?>
}