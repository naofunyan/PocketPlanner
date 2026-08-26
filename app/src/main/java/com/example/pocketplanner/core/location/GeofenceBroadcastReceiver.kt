package com.example.pocketplanner.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pocketplanner.core.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper

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
                }
            }
        }
    }
}