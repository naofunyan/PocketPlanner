package com.example.pocketplanner.data.sync

import android.util.Log
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.TripEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val tripDao: TripDao
) {
    // We store trips in a top-level collection called "trips"
    private val tripsCollection = firestore.collection("trips")

    /**
     * Pushes a local trip to Firestore
     */
    suspend fun pushTripToCloud(trip: TripEntity) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("SyncManager", "User not logged in, skipping sync.")
            return
        }

        try {
            // Push the data to Firestore
            tripsCollection.document(trip.id).set(trip).await()

            // Mark it as synced locally
            tripDao.insertTrip(trip.copy(isSyncedWithCloud = true))
            Log.d("SyncManager", "Successfully pushed trip ${trip.id} to cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to push trip: ${e.message}")
        }
    }

    /**
     * Pulls all trips from Firestore for the current user and saves them locally
     */
    suspend fun pullTripsFromCloud() {
        val user = auth.currentUser ?: return

        try {
            // Get all trips belonging to this user
            val snapshot = tripsCollection.whereEqualTo("userId", user.uid).get().await()

            for (document in snapshot.documents) {
                // Convert Firestore document back to our TripEntity
                val trip = document.toObject(TripEntity::class.java)
                if (trip != null) {
                    // Save it to our local Room database
                    tripDao.insertTrip(trip.copy(isSyncedWithCloud = true))
                }
            }
            Log.d("SyncManager", "Successfully pulled trips from cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to pull trips: ${e.message}")
        }
    }

    /**
     * Deletes a trip from Firestore
     */
    suspend fun deleteTripFromCloud(tripId: String) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("SyncManager", "User not logged in, skipping cloud delete.")
            return
        }

        try {
            tripsCollection.document(tripId).delete().await()
            Log.d("SyncManager", "Successfully deleted trip $tripId from cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to delete trip from cloud: ${e.message}")
        }
    }
}