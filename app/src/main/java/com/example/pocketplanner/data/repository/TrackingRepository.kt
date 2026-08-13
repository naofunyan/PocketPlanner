package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.TrackingPointDao
import com.example.pocketplanner.data.local.entity.TrackingPointEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    private val trackingPointDao: TrackingPointDao
) {
    suspend fun addTrackingPoint(point: TrackingPointEntity) {
        trackingPointDao.insertPoint(point)
    }

    fun getTrackingPoints(tripId: String) = trackingPointDao.getPointsForTrip(tripId)
}