package com.example.pocketplanner.ui.tracking

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.core.location.LocationTrackingService
import com.example.pocketplanner.data.local.entity.TrackingPointEntity
import com.example.pocketplanner.data.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _trackingPoints = MutableStateFlow<List<TrackingPointEntity>>(emptyList())
    val trackingPoints: StateFlow<List<TrackingPointEntity>> = _trackingPoints.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    fun loadTripPoints(tripId: String) {
        viewModelScope.launch {
            trackingRepository.getTrackingPoints(tripId).collect { points ->
                _trackingPoints.value = points
            }
        }
    }

    fun startTracking(context: Context, tripId: String) {
        _isTracking.value = true
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra(LocationTrackingService.EXTRA_TRIP_ID, tripId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking(context: Context) {
        _isTracking.value = false
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }
}