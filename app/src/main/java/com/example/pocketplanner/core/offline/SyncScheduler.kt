package com.example.pocketplanner.core.offline

import android.content.Context
import androidx.work.*
import com.example.pocketplanner.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "FirestoreSyncWork",
                ExistingWorkPolicy.KEEP, // Don't replace if it's already pending
                syncRequest
            )
    }
}