package com.example.pocketplanner.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.dao.AlertDao
import com.example.pocketplanner.data.local.entity.AlertEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertDao: AlertDao
) : ViewModel() {

    val alerts: StateFlow<List<AlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun markAllAsRead() {
        viewModelScope.launch {
            alertDao.markAllAsRead()
        }
    }
}