package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.TrackingPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackingPointEntity)

    // Flow automatically updates the UI when a new location point is tracked
    @Query("SELECT * FROM tracking_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getPointsForTrip(tripId: String): Flow<List<TrackingPointEntity>>
}