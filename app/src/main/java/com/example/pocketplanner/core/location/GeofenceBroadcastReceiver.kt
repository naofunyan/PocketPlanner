package com.example.pocketplanner.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pocketplanner.core.notification.NotificationHelper
import com.example.pocketplanner.data.local.dao.AlertDao
import com.example.pocketplanner.data.local.entity.AlertEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alertDao: AlertDao

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (!triggeringGeofences.isNullOrEmpty()) {
                for (geofence in triggeringGeofences) {
                    val placeName = geofence.requestId
                    val message = "You are near $placeName! Check it out."

                    // 1. Show System Push Notification
                    notificationHelper.showProximityAlert(placeName)

                    // 2. Save to Alerts Database for the in-app screen
                    CoroutineScope(Dispatchers.IO).launch {
                        alertDao.insertAlert(
                            AlertEntity(
                                id = UUID.randomUUID().toString(),
                                placeName = placeName,
                                message = message,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}