package com.example.pocketplanner.core.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.pocketplanner.data.local.entity.PlaceEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient
) {
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun addGeofencesForPlaces(places: List<PlaceEntity>) {
        if (places.isEmpty()) return

        val geofences = places.map { place ->
            Geofence.Builder()
                .setRequestId(place.name) // Use the name so we can easily display it in the receiver
                .setCircularRegion(place.lat, place.lng, 200f) // 200 meters radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        // Requires ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION
        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}