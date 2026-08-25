package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.PlaceDao
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.core.offline.SyncScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val placeDao: PlaceDao,
    private val syncScheduler: SyncScheduler
) {
    fun getAllTrips(userId: String): Flow<List<TripEntity>> {
        return tripDao.getAllTripsForUser(userId)
    }

    suspend fun createTrip(trip: TripEntity) {
        val newTrip = trip.copy(isSyncedWithCloud = false, updatedAt = System.currentTimeMillis())
        tripDao.insertTrip(newTrip)
        syncScheduler.scheduleSync()
    }

    suspend fun updateTrip(trip: TripEntity) {
        val updatedTrip = trip.copy(isSyncedWithCloud = false, updatedAt = System.currentTimeMillis())
        tripDao.insertTrip(updatedTrip)
        syncScheduler.scheduleSync()
    }

    fun getTrip(tripId: String): Flow<TripEntity?> {
        return tripDao.getTripFlow(tripId)
    }

    suspend fun deleteTrip(tripId: String) {
        // Soft Delete locally first for immediate UI update
        tripDao.deleteTripById(tripId)
        syncScheduler.scheduleSync()
    }
    suspend fun savePlaces(places: List<PlaceEntity>) {
        val unsyncedPlaces = places.map { it.copy(isSyncedWithCloud = false, updatedAt = System.currentTimeMillis()) }
        placeDao.insertPlaces(unsyncedPlaces)
        syncScheduler.scheduleSync()
    }
    fun getPlacesForDay(tripId: String, dayNumber: Int): Flow<List<PlaceEntity>> {
        return placeDao.getPlacesForDay(tripId, dayNumber)
    }
    fun getAllPlacesForTrip(tripId: String): Flow<List<PlaceEntity>> {
        return placeDao.getAllPlacesForTrip(tripId)
    }
    suspend fun deletePlacesForDaysGreaterThan(tripId: String, maxDayNumber: Int) {
        placeDao.deletePlacesForDaysGreaterThan(tripId, maxDayNumber)
        syncScheduler.scheduleSync()
    }
}