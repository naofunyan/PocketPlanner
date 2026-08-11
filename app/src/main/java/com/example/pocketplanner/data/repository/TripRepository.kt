package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val syncManager: FirestoreSyncManager
) {
    fun getAllTrips(userId: String): Flow<List<TripEntity>> {
        return tripDao.getAllTripsForUser(userId)
    }

    suspend fun createTrip(trip: TripEntity) {
        // Save locally for immediate offline use
        tripDao.insertTrip(trip)
        // Push to the cloud in the background
        syncManager.pushTripToCloud(trip)
    }
}