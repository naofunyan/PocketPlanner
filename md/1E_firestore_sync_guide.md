# Step 1E: Cloud Firestore Sync Layer

Now that we have local data (Room) and authentication (Firebase Auth), we need a way to sync our user's trips to the cloud so they don't lose their data if they switch phones!

We'll build a `FirestoreSyncManager` that acts as a bridge between your local Room database and Firebase Firestore.

## 1. Enable Firestore in the Console
Before we write code, we need to turn on the database in your Firebase Console.
1. Go back to your [Firebase Console](https://console.firebase.google.com/).
2. On the left menu, click **Build > Firestore Database**.
3. Click **Create database**.
4. Choose **Start in test mode** (this allows us to read/write without complex security rules while developing).
5. Click Next and choose a location close to you, then click **Enable**.

## 2. Create the Sync Manager
1. In Android Studio, go to `com.example.pocketplanner.data` and create a new Package named `sync`.
2. Inside `sync`, create a Kotlin Class named `FirestoreSyncManager.kt`:

```kotlin
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
}
```

## 3. Provide Firestore in Hilt
Just like we did for `FirebaseAuth`, we need to tell Hilt how to provide `FirebaseFirestore` so our SyncManager can use it.

1. Open `com.example.pocketplanner.data.di.AppModule.kt`.
2. Add this new `@Provides` function inside your `FirebaseModule` object:

```kotlin
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
```

(Make sure you click `Alt + Enter` to import `com.google.firebase.firestore.FirebaseFirestore` if it turns red!)

---
That's it! Let me know when you have added these files and enabled Firestore in the console.
