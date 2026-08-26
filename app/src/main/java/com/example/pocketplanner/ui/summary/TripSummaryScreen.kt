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
import androidx.compose.ui.res.stringResource
import com.example.pocketplanner.R
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

    val failedFormatStr = stringResource(R.string.summary_failed)
    val shareTextFormatStr = stringResource(R.string.summary_share_text)
    val shareLinkStr = stringResource(R.string.summary_share_link)


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.summary_back_cd))
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
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.summary_share_cd))
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
                text = trip?.name.takeIf { !it.isNullOrBlank() } ?: String.format(stringResource(R.string.summary_trip_to), trip?.destination ?: ""),
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
                        Text(stringResource(R.string.summary_ai_diary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (trip?.aiDiary.isNullOrBlank()) {
                        Text(stringResource(R.string.summary_memories_waiting), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                viewModel.generateDiary(
                                    onError = { err ->
                                        android.widget.Toast.makeText(context, String.format(failedFormatStr, err), android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isGeneratingDiary
                        ) {
                            if (isGeneratingDiary) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.summary_writing))
                            } else {
                                Text(stringResource(R.string.summary_generate_diary))
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
                    Text(stringResource(R.string.summary_financial), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.summary_budget))
                        Text(currencyFormatter.format(budget), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.summary_total_spent))
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
                        Text(stringResource(R.string.summary_export_csv))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Place Ratings Section
            if (places.isNotEmpty()) {
                Text(stringResource(R.string.summary_rate_places), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            title = { Text(stringResource(R.string.summary_trip_published)) },
            text = {
                Column {
                    Text(stringResource(R.string.summary_published_desc))
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
                        putExtra(Intent.EXTRA_TEXT, String.format(shareTextFormatStr, showShareDialog!!))
                    }
                    context.startActivity(Intent.createChooser(intent, shareLinkStr))
                    showShareDialog = null
                }) {
                    Text(stringResource(R.string.summary_share_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = null }) {
                    Text(stringResource(R.string.summary_close_btn))
                }
            }
        )
    }

    if (shareError != null) {
        AlertDialog(
            onDismissRequest = { shareError = null },
            title = { Text(stringResource(R.string.summary_error)) },
            text = { Text(shareError!!) },
            confirmButton = { TextButton(onClick = { shareError = null }) { Text(stringResource(R.string.summary_ok)) } }
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
                        contentDescription = String.format(stringResource(R.string.summary_rate_cd), i),
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
