package com.example.pocketplanner.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pocketplanner.data.sync.FirestoreSyncManager
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error

@HiltViewModel
class ImportTripViewModel @Inject constructor(
    private val syncManager: FirestoreSyncManager
) : ViewModel() {

    fun importSharedTrip(tripId: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                syncManager.pullSharedTripFromCloud(tripId)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to import trip")
                }
            }
        }
    }
}

@Composable
fun ImportTripScreen(
    tripId: String,
    onImportComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ImportTripViewModel = hiltViewModel()
) {
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId) {
        isImporting = true
        viewModel.importSharedTrip(
            tripId = tripId,
            onComplete = {
                isImporting = false
                onImportComplete()
            },
            onError = { msg ->
                isImporting = false
                errorMessage = msg
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isImporting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // DYNAMIC COLOR
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Importing Trip...",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please wait while we download the itinerary details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (errorMessage != null) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to Import Trip",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground // DYNAMIC TEXT
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // DYNAMIC BUTTON
                        contentColor = MaterialTheme.colorScheme.onPrimary // DYNAMIC TEXT
                    )
                ) {
                    Text("Go Home")
                }
            }
        }
    }
}