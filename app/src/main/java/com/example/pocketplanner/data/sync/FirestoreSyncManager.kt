package com.example.pocketplanner.data.sync

import android.util.Log
import com.example.pocketplanner.data.local.dao.TripDao
import com.example.pocketplanner.data.local.entity.TripEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.pocketplanner.data.local.entity.PlaceEntity
import android.content.Context
import com.example.pocketplanner.data.local.dao.TicketDao
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val tripDao: TripDao,
    private val placeDao: com.example.pocketplanner.data.local.dao.PlaceDao,
    private val ticketDao: TicketDao,                          // ← ADD
    private val storage: FirebaseStorage,                       // ← ADD
    @dagger.hilt.android.qualifiers.ApplicationContext          // ← ADD
    private val appContext: Context                              // ← ADD
) {
    // We store trips in a top-level collection called "trips"
    private val tripsCollection = firestore.collection("trips")
    private val ticketsCollection = firestore.collection("tickets")  // ← ADD
    private val storageRef = storage.reference                       // ← ADD

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

    // ============================================================
    //  TICKET SYNC
    // ============================================================

    /**
     * Pushes a local ticket to Firestore + uploads images to Firebase Storage.
     */
    suspend fun pushTicketToCloud(ticket: TicketEntity) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("SyncManager", "User not logged in, skipping ticket sync.")
            return
        }

        try {
            // 1. Upload original image to Firebase Storage
            val imageFile = File(ticket.imageUri)
            var imageUrl = ""
            if (imageFile.exists()) {
                val imageRef = storageRef.child("tickets/${user.uid}/${ticket.id}_original.jpg")
                imageRef.putFile(android.net.Uri.fromFile(imageFile)).await()
                imageUrl = imageRef.downloadUrl.await().toString()
            }

            // 2. Upload thumbnail (optional)
            var thumbUrl = ""
            ticket.thumbnailUri?.let { thumbPath ->
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) {
                    val thumbRef = storageRef.child("tickets/${user.uid}/${ticket.id}_thumb.jpg")
                    thumbRef.putFile(android.net.Uri.fromFile(thumbFile)).await()
                    thumbUrl = thumbRef.downloadUrl.await().toString()
                }
            }

            // 3. Create a Firestore-friendly map (replace local paths with URLs)
            val ticketData = hashMapOf(
                "id" to ticket.id,
                "userId" to user.uid,
                "tripId" to ticket.tripId,
                "title" to ticket.title,
                "type" to ticket.type,
                "dateTime" to ticket.dateTime,
                "imageUrl" to imageUrl,
                "thumbnailUrl" to thumbUrl,
                "qrContent" to ticket.qrContent,
                "ocrRawText" to ticket.ocrRawText,
                "confirmationCode" to ticket.confirmationCode,
                "notes" to ticket.notes,
                "createdAt" to ticket.createdAt
            )

            ticketsCollection.document(ticket.id).set(ticketData).await()

            // 4. Mark as synced locally
            ticketDao.insertTicket(ticket.copy(isSyncedWithCloud = true, userId = user.uid))
            Log.d("SyncManager", "Successfully pushed ticket ${ticket.id} to cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to push ticket: ${e.message}")
        }
    }

    /**
     * Pulls all tickets from Firestore for the current user and saves them locally.
     * Downloads images from Firebase Storage to local storage.
     */
    suspend fun pullTicketsFromCloud() {
        val user = auth.currentUser ?: return

        try {
            val snapshot = ticketsCollection.whereEqualTo("userId", user.uid).get().await()

            for (document in snapshot.documents) {
                val ticketId = document.getString("id") ?: continue

                // Skip if already exists locally and is synced
                val existingTicket = ticketDao.getTicketByIdDirect(ticketId)
                if (existingTicket != null && existingTicket.isSyncedWithCloud) continue

                // Download original image from Storage
                val imageUrl = document.getString("imageUrl") ?: ""
                var localImagePath = ""
                if (imageUrl.isNotBlank()) {
                    localImagePath = downloadImageFromStorage(imageUrl, "${ticketId}_original.jpg")
                }

                // Download thumbnail
                val thumbUrl = document.getString("thumbnailUrl") ?: ""
                var localThumbPath: String? = null
                if (thumbUrl.isNotBlank()) {
                    localThumbPath = downloadImageFromStorage(thumbUrl, "${ticketId}_thumb.jpg")
                }

                val ticket = TicketEntity(
                    id = ticketId,
                    userId = user.uid,
                    tripId = document.getString("tripId"),
                    title = document.getString("title") ?: "Untitled",
                    type = document.getString("type") ?: "Other",
                    dateTime = document.getLong("dateTime") ?: 0L,
                    imageUri = localImagePath,
                    thumbnailUri = localThumbPath,
                    qrContent = document.getString("qrContent"),
                    ocrRawText = document.getString("ocrRawText"),
                    confirmationCode = document.getString("confirmationCode"),
                    notes = document.getString("notes") ?: "",
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                    isSyncedWithCloud = true
                )
                ticketDao.insertTicket(ticket)
            }
            Log.d("SyncManager", "Successfully pulled tickets from cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to pull tickets: ${e.message}")
        }
    }

    /**
     * Deletes a ticket from Firestore and its images from Storage.
     */
    suspend fun deleteTicketFromCloud(ticketId: String) {
        val user = auth.currentUser ?: return

        try {
            ticketsCollection.document(ticketId).delete().await()

            // Also delete images from Storage (ignore errors — they may not exist)
            try {
                storageRef.child("tickets/${user.uid}/${ticketId}_original.jpg").delete().await()
                storageRef.child("tickets/${user.uid}/${ticketId}_thumb.jpg").delete().await()
            } catch (_: Exception) { }

            Log.d("SyncManager", "Successfully deleted ticket $ticketId from cloud.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to delete ticket from cloud: ${e.message}")
        }
    }

    /**
     * Downloads an image from a Firebase Storage URL to local internal storage.
     */
    private suspend fun downloadImageFromStorage(url: String, fileName: String): String {
        val ticketsDir = File(appContext.filesDir, "tickets").also { it.mkdirs() }
        val destFile = File(ticketsDir, fileName)

        try {
            val ref = storage.getReferenceFromUrl(url)
            val bytes = ref.getBytes(10 * 1024 * 1024).await() // Max 10MB
            FileOutputStream(destFile).use { it.write(bytes) }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to download image: ${e.message}")
        }

        return destFile.absolutePath
    }
}