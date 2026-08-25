package com.example.pocketplanner.ui.summary

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketplanner.data.local.entity.PlaceEntity
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: TripSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) {
        viewModel.loadTripSummary(tripId)
    }

    val trip by viewModel.trip.collectAsState()
    val places by viewModel.places.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val isGeneratingDiary by viewModel.isGeneratingDiary.collectAsState()

    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf<String?>(null) }
    var shareError by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val totalSpent = expenses.sumOf { it.convertedAmountVND }
    val budget = trip?.budget ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp))
                    } else {
                        IconButton(onClick = {
                            isPublishing = true
                            viewModel.publishTrip(
                                onSuccess = { link ->
                                    isPublishing = false
                                    showShareDialog = link
                                },
                                onError = { err ->
                                    isPublishing = false
                                    shareError = err
                                }
                            )
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Trip")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = trip?.name.takeIf { !it.isNullOrBlank() } ?: "Trip to ${trip?.destination ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // AI Diary Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Travel Diary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (trip?.aiDiary.isNullOrBlank()) {
                        Text("Your memories are waiting to be written.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                viewModel.generateDiary(
                                    onError = { err ->
                                        android.widget.Toast.makeText(context, "Failed: $err", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isGeneratingDiary
                        ) {
                            if (isGeneratingDiary) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Writing...")
                            } else {
                                Text("Generate Diary")
                            }
                        }
                    } else {
                        Text(trip?.aiDiary ?: "", style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expenses Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Budget:")
                        Text(currencyFormatter.format(budget), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Spent:")
                        Text(
                            text = currencyFormatter.format(totalSpent),
                            fontWeight = FontWeight.Bold,
                            color = if (totalSpent > budget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        val uri = viewModel.exportExpensesToCsv(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Expenses"))
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Export Expenses (CSV)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Place Ratings Section
            if (places.isNotEmpty()) {
                Text("Rate Your Places", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(places) { place ->
                        PlaceRatingCard(place = place, onRate = { rating -> viewModel.ratePlace(place, rating) })
                    }
                }
            }
        }
    }

    if (showShareDialog != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = null },
            title = { Text("Trip Published!") },
            text = {
                Column {
                    Text("Your trip has been published. Share this link with your friends:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(showShareDialog!!, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out my trip itinerary on PocketPlanner! ${showShareDialog!!}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Link"))
                    showShareDialog = null
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (shareError != null) {
        AlertDialog(
            onDismissRequest = { shareError = null },
            title = { Text("Error") },
            text = { Text(shareError!!) },
            confirmButton = { TextButton(onClick = { shareError = null }) { Text("OK") } }
        )
    }
}

@Composable
fun PlaceRatingCard(place: PlaceEntity, onRate: (Int) -> Unit) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(place.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(place.category, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                val currentRating = place.userRating ?: 0
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Rate $i",
                        tint = if (i <= currentRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onRate(i) }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}
