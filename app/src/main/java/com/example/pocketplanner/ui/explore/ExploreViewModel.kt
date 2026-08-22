package com.example.pocketplanner.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.dao.SavedPlaceDao
import com.example.pocketplanner.data.local.entity.SavedPlaceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val savedPlaceDao: SavedPlaceDao
) : ViewModel() {

    // 1. This Flow constantly watches the database and updates the UI automatically
    val savedPlaces: StateFlow<List<String>> = savedPlaceDao.getAllSavedPlaces()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. A single helper function to handle both saving and removing
    fun toggleSavePlace(placeName: String, isCurrentlySaved: Boolean) {
        viewModelScope.launch {
            if (isCurrentlySaved) {
                savedPlaceDao.deleteSavedPlace(placeName)
            } else {
                savedPlaceDao.insertSavedPlace(SavedPlaceEntity(placeName))
            }
        }
    }
}