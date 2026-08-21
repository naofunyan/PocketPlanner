package com.example.pocketplanner.ui.expense

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.TicketRepository
import com.example.pocketplanner.data.repository.TripRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

// ---------- Filter model ----------

sealed class TicketFilter {
    object All : TicketFilter()
    object General : TicketFilter()
    data class Trip(val tripId: String, val tripName: String) : TicketFilter()
}

// ---------- OCR result model ----------

data class OcrResult(
    val rawText: String = "",
    val suggestedTitle: String = "",
    val suggestedConfirmationCode: String = "",
    val suggestedDate: Long? = null
)

// ---------- ViewModel ----------

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TicketViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val tripRepository: TripRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // ---- Trips (for filter chips + "Link to trip" dropdown) ----

    private val _allTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val allTrips: StateFlow<List<TripEntity>> = _allTrips.asStateFlow()

    fun loadTrips(userId: String) {
        viewModelScope.launch {
            tripRepository.getAllTrips(userId).collect { trips ->
                _allTrips.value = trips
            }
        }
    }

    // ---- Filtering ----

    private val _selectedFilter = MutableStateFlow<TicketFilter>(TicketFilter.All)
    val selectedFilter: StateFlow<TicketFilter> = _selectedFilter.asStateFlow()

    fun setFilter(filter: TicketFilter) {
        _selectedFilter.value = filter
    }

    // Base: all tickets from the database
    private val _allTickets: Flow<List<TicketEntity>> = ticketRepository.getAllTickets()

    // Derived: filtered list based on the selected filter chip
    val filteredTickets: Flow<List<TicketEntity>> = _selectedFilter.flatMapLatest { filter ->
        when (filter) {
            is TicketFilter.All -> ticketRepository.getAllTickets()
            is TicketFilter.General -> ticketRepository.getUnlinkedTickets()
            is TicketFilter.Trip -> ticketRepository.getTicketsForTrip(filter.tripId)
        }
    }

    // ---- CRUD ----

    fun addTicket(
        title: String,
        type: String,
        dateTime: Long,
        imageUri: Uri,
        tripId: String?,
        confirmationCode: String?,
        notes: String,
        ocrRawText: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val ticketId = UUID.randomUUID().toString()

            // 1. Copy original image to permanent internal storage (no re-encoding!)
            val permanentPath = copyImageToInternalStorage(imageUri, ticketId)

            // 2. Generate a thumbnail for the list view
            val thumbPath = generateThumbnail(permanentPath, ticketId)

            // 3. Create and insert the entity
            val ticket = TicketEntity(
                id = ticketId,
                tripId = tripId,
                title = title,
                type = type,
                dateTime = dateTime,
                imageUri = permanentPath,
                thumbnailUri = thumbPath,
                confirmationCode = confirmationCode,
                notes = notes,
                ocrRawText = ocrRawText
            )
            ticketRepository.addTicket(ticket)
        }
    }

    fun deleteTicket(ticket: TicketEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clean up stored image files
            try {
                File(ticket.imageUri).delete()
                ticket.thumbnailUri?.let { File(it).delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ticketRepository.deleteTicket(ticket)
        }
    }

    // ---- Image storage (QR-safe) ----

    /**
     * Copies raw bytes from a content URI to internal storage.
     * No Bitmap.compress(), no re-encoding — QR codes stay pixel-perfect.
     */
    private fun copyImageToInternalStorage(sourceUri: Uri, ticketId: String): String {
        val ticketsDir = File(appContext.filesDir, "tickets").also { it.mkdirs() }
        val destFile = File(ticketsDir, "${ticketId}_original.jpg")

        appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        return destFile.absolutePath
    }

    /**
     * Generates a 400px-wide thumbnail JPEG for the list view.
     * This is the ONLY place we use Bitmap — and only for the thumbnail, never the original.
     */
    private fun generateThumbnail(originalPath: String, ticketId: String): String? {
        return try {
            val ticketsDir = File(appContext.filesDir, "tickets")
            val thumbFile = File(ticketsDir, "${ticketId}_thumb.jpg")

            // Decode bounds first to calculate the sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(originalPath, options)

            val targetWidth = 400
            val sampleSize = (options.outWidth / targetWidth).coerceAtLeast(1)

            // Decode with downsampling
            val bitmap = BitmapFactory.Options().let { opts ->
                opts.inSampleSize = sampleSize
                BitmapFactory.decodeFile(originalPath, opts)
            } ?: return null

            // Scale to exact width
            val scaledHeight = (bitmap.height * (targetWidth.toFloat() / bitmap.width)).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, scaledHeight, true)

            FileOutputStream(thumbFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            bitmap.recycle()
            scaledBitmap.recycle()

            thumbFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ---- OCR ----

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * Runs ML Kit Text Recognition on the given image URI.
     * Returns an OcrResult with raw text and auto-detected fields.
     */
    suspend fun runOcr(imageUri: Uri): OcrResult = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val image = InputImage.fromFilePath(appContext, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val visionText = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        continuation.resumeWith(Result.success(text))
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            val rawText = visionText.text

            // --- TITLE: Find the largest text block by bounding box area ---
            // This works because event names / flight info are usually the
            // biggest, most prominent text on any ticket.
            val suggestedTitle = visionText.textBlocks
                .filter { block ->
                    val text = block.text.trim()
                    // Skip junk: pure numbers, short strings, time patterns
                    text.length > 3
                            && !text.matches(Regex("^\\d{1,2}[.:]+\\d{2}.*"))  // skip "18:35", "1:00 PM"
                            && !text.matches(Regex("^\\d+$"))                   // skip pure numbers
                            && !text.matches(Regex("^[\\d\\s%°.:]+$"))          // skip status bar junk
                }
                .maxByOrNull { block ->
                    // Pick the block with the largest bounding box area
                    val box = block.boundingBox
                    if (box != null) box.width() * box.height() else 0
                }
                ?.text
                ?.lines()
                // Take up to the first 2 lines (titles can wrap)
                ?.take(2)
                ?.joinToString(" ")
                ?.trim()
                ?: ""

            // --- CONFIRMATION CODE: Look near label keywords ---
            // Scan for text blocks near "ORDER NUMBER", "BOOKING", "CONFIRMATION", etc.
            val labelKeywords = listOf("order number", "booking", "confirmation", "conf", "reference", "ticket")
            val allBlockTexts = visionText.textBlocks.map { it.text }

            var suggestedConfCode = ""
            for (block in visionText.textBlocks) {
                val blockLower = block.text.lowercase()
                if (labelKeywords.any { keyword -> blockLower.contains(keyword) }) {
                    // Found a label block — extract the alphanumeric code from it
                    val codeRegex = Regex("[A-Z0-9]{5,15}")
                    val match = codeRegex.find(block.text.uppercase())
                    if (match != null) {
                        suggestedConfCode = match.value
                        break
                    }
                }
            }
            // Fallback: if no label found, search all blocks for a standalone code
            if (suggestedConfCode.isBlank()) {
                val codeRegex = Regex("[A-Z]{2,5}\\d{4,10}|[A-Z0-9]{6,12}")
                for (block in visionText.textBlocks) {
                    val match = codeRegex.find(block.text.uppercase())
                    if (match != null) {
                        suggestedConfCode = match.value
                        break
                    }
                }
            }

            // --- DATE: Try to parse common date patterns ---
            val datePatterns = listOf(
                // "APR 18, 2026" or "Aug 25, 2026"
                Regex("([A-Za-z]{3,9})\\s+(\\d{1,2}),?\\s+(\\d{4})") to "MMM dd yyyy",
                // "18 APR 2026" or "25 Aug 2026"
                Regex("(\\d{1,2})\\s+([A-Za-z]{3,9})\\s+(\\d{4})") to "dd MMM yyyy",
                // "2026-04-18" or "2026/04/18"
                Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})") to "yyyy-MM-dd",
                // "18/04/2026" or "18-04-2026"
                Regex("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})") to "dd/MM/yyyy"
            )

            var suggestedDate: Long? = null
            for ((regex, pattern) in datePatterns) {
                val match = regex.find(rawText)
                if (match != null) {
                    try {
                        val formatter = java.text.SimpleDateFormat(
                            pattern,
                            java.util.Locale.ENGLISH
                        )
                        suggestedDate = formatter.parse(
                            match.value.replace(",", "").replace("/", "-").let { raw ->
                                // Normalize for the formatter
                                if (pattern == "dd/MM/yyyy") raw.replace("-", "/") else raw
                            }
                        )?.time
                        if (suggestedDate != null) break
                    } catch (_: Exception) { }
                }
            }

            OcrResult(
                rawText = rawText,
                suggestedTitle = suggestedTitle,
                suggestedConfirmationCode = suggestedConfCode,
                suggestedDate = suggestedDate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult()
        } finally {
            _isScanning.value = false
        }
    }
}