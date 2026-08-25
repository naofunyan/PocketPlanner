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
    @Query("SELECT * FROM trips WHERE userId = :userId AND isDeleted = 0 ORDER BY startDate ASC")
    fun getAllTripsForUser(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId AND isDeleted = 0")
    fun getTripFlow(tripId: String): Flow<TripEntity?>

    // Soft delete
    @Query("UPDATE trips SET isDeleted = 1, isSyncedWithCloud = 0, updatedAt = :timestamp WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String, timestamp: Long = System.currentTimeMillis())
    
    // For syncing
    @Query("SELECT * FROM trips WHERE userId = :userId AND isSyncedWithCloud = 0")
    suspend fun getUnsyncedTrips(userId: String): List<TripEntity>
    
    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripByIdDirect(tripId: String): TripEntity?
}