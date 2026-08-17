package com.example.pocketplanner.data.sync

import android.util.Log
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.TripEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.pocketplanner.data.local.entity.PlaceEntity

class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val tripDao: TripDao,
    private val placeDao: com.example.pocketplanner.data.local.dao.PlaceDao
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
            val snapshot = tripsCollection.whereEqualTo("userId", user.uid).get().await()

            for (document in snapshot.documents) {
                val trip = document.toObject(TripEntity::class.java)
                if (trip != null) {
                    tripDao.insertTrip(trip.copy(isSyncedWithCloud = true))
                    
                    // Also pull places
                    val placesSnapshot = document.reference.collection("places").get().await()
                    val placesList = placesSnapshot.documents.mapNotNull { it.toObject(PlaceEntity::class.java) }
                    if (placesList.isNotEmpty()) {
                        placeDao.insertPlaces(placesList)
                    }
                }
            }
            Log.d("SyncManager", "Successfully pulled trips from cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to pull trips: ${e.message}")
        }
    }

    suspend fun pushPlacesToCloud(tripId: String, places: List<PlaceEntity>) {
        if (auth.currentUser == null) return
        try {
            val batch = firestore.batch()
            val placesRef = tripsCollection.document(tripId).collection("places")
            places.forEach { place ->
                batch.set(placesRef.document(place.id), place)
            }
            batch.commit().await()
            Log.d("SyncManager", "Successfully pushed places to cloud for trip $tripId")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to push places: ${e.message}")
        }
    }

    suspend fun pullSharedTripFromCloud(tripId: String) {
        try {
            val doc = tripsCollection.document(tripId).get().await()
            if (doc.exists()) {
                val trip = doc.toObject(TripEntity::class.java)
                if (trip != null) {
                    // Save as current user's trip so they can edit their own copy
                    val currentUser = auth.currentUser
                    val newTripId = java.util.UUID.randomUUID().toString()
                    val importedTrip = trip.copy(
                        id = newTripId, 
                        userId = currentUser?.uid ?: "",
                        isSyncedWithCloud = false
                    )
                    tripDao.insertTrip(importedTrip)
                    
                    val placesSnapshot = doc.reference.collection("places").get().await()
                    val placesList = placesSnapshot.documents.mapNotNull { it.toObject(PlaceEntity::class.java)?.copy(tripId = newTripId) }
                    if (placesList.isNotEmpty()) {
                        placeDao.insertPlaces(placesList)
                    }
                    
                    // Sync the newly imported trip to the cloud under their own account
                    if (currentUser != null) {
                        pushTripToCloud(importedTrip)
                        pushPlacesToCloud(newTripId, placesList)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to pull shared trip: ${e.message}")
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