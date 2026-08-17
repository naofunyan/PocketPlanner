package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val placeDao: PlaceDao,
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

    fun getTrip(tripId: String): Flow<TripEntity?> {
        return tripDao.getTripFlow(tripId)
    }

    suspend fun deleteTrip(tripId: String) {
        // Delete locally first for immediate UI update
        tripDao.deleteTripById(tripId)
        // Push delete to the cloud
        syncManager.deleteTripFromCloud(tripId)
    }
    suspend fun savePlaces(places: List<PlaceEntity>) {
        placeDao.insertPlaces(places)
    }
    fun getPlacesForDay(tripId: String, dayNumber: Int): Flow<List<PlaceEntity>> {
        return placeDao.getPlacesForDay(tripId, dayNumber)
    }
    fun getAllPlacesForTrip(tripId: String): Flow<List<PlaceEntity>> {
        return placeDao.getAllPlacesForTrip(tripId)
    }
    suspend fun deletePlacesForDaysGreaterThan(tripId: String, maxDayNumber: Int) {
        placeDao.deletePlacesForDaysGreaterThan(tripId, maxDayNumber)
    }
}