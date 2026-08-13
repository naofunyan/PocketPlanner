package com.example.pocketplanner.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: FirestoreSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // For the hackathon scope, we just pull remote changes to ensure we have the latest.
            // Pushing local changes requires a more complex diffing mechanism.
            syncManager.pullTripsFromCloud()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If it fails (e.g., connection drops mid-sync), tell WorkManager to retry later
            Result.retry()
        }
    }
}