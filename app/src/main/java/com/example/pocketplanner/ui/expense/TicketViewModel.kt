package com.example.pocketplanner.ui.expense

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.data.local.entity.TripEntity
import com.example.pocketplanner.data.repository.TicketRepository
import com.example.pocketplanner.data.repository.TripRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Color as AndroidColor
import com.example.pocketplanner.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ---------- Filter model ----------

sealed class TicketFilter {
    object All : TicketFilter()
    object General : TicketFilter()
    data class Trip(val tripId: String, val tripName: String) : TicketFilter()
}

// ---------- Ticket status model ----------

sealed class TicketStatus(val sortOrder: Int) {
    object Today : TicketStatus(0)
    object Tomorrow : TicketStatus(1)
    data class Upcoming(val daysUntil: Int) : TicketStatus(2)
    data class Later(val displayDate: String) : TicketStatus(3)
    object Expired : TicketStatus(4)

    companion object {
        fun fromDateTime(dateTimeMillis: Long): TicketStatus {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply { timeInMillis = dateTimeMillis }

            // Zero out time for day comparison
            val todayStart = now.clone() as java.util.Calendar
            todayStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
            todayStart.set(java.util.Calendar.MINUTE, 0)
            todayStart.set(java.util.Calendar.SECOND, 0)
            todayStart.set(java.util.Calendar.MILLISECOND, 0)

            val targetStart = target.clone() as java.util.Calendar
            targetStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
            targetStart.set(java.util.Calendar.MINUTE, 0)
            targetStart.set(java.util.Calendar.SECOND, 0)
            targetStart.set(java.util.Calendar.MILLISECOND, 0)

            val dayDiff = ((targetStart.timeInMillis - todayStart.timeInMillis) / 86400000L).toInt()

            return when {
                dayDiff < 0 -> Expired
                dayDiff == 0 -> Today
                dayDiff == 1 -> Tomorrow
                dayDiff in 2..30 -> Upcoming(dayDiff)
                else -> {
                    val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    Later(formatter.format(java.util.Date(dateTimeMillis)))
                }
            }
        }
    }
}

// ---------- OCR result model ----------

data class OcrResult(
    val rawText: String = "",
    val suggestedTitle: String = "",
    val suggestedConfirmationCode: String = "",
    val suggestedDate: Long? = null,
    val suggestedType: String = ""
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

    // ---- Search ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Base: all tickets from the database
    private val _allTickets: Flow<List<TicketEntity>> = ticketRepository.getAllTickets()

    // Derived: filtered by chip + search query, sorted by status (active first)
    val filteredTickets: Flow<List<TicketEntity>> = combine(
        _selectedFilter.flatMapLatest { filter ->
            when (filter) {
                is TicketFilter.All -> ticketRepository.getAllTickets()
                is TicketFilter.General -> ticketRepository.getUnlinkedTickets()
                is TicketFilter.Trip -> ticketRepository.getTicketsForTrip(filter.tripId)
            }
        },
        _searchQuery
    ) { tickets, query ->
        val filtered = if (query.isBlank()) {
            tickets
        } else {
            val lowerQuery = query.lowercase()
            tickets.filter { ticket ->
                ticket.title.lowercase().contains(lowerQuery)
                        || ticket.confirmationCode?.lowercase()?.contains(lowerQuery) == true
                        || ticket.ocrRawText?.lowercase()?.contains(lowerQuery) == true
                        || ticket.qrContent?.lowercase()?.contains(lowerQuery) == true
                        || ticket.type.lowercase().contains(lowerQuery)
                        || ticket.notes.lowercase().contains(lowerQuery)
            }
        }

        // Sort: Today → Tomorrow → Upcoming → Later → Expired
        // Within same status, sort by dateTime ascending (soonest first)
        filtered.sortedWith(compareBy<TicketEntity> {
            TicketStatus.fromDateTime(it.dateTime).sortOrder
        }.thenBy {
            it.dateTime
        })
    }

    /**
     * Generates a Bitmap QR code from a raw text string.
     * Used as a backup when the original ticket image QR is unreadable.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        ocrRawText: String?,
        qrContent: String? = null
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
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                tripId = tripId,
                title = title,
                type = type,
                dateTime = dateTime,
                imageUri = permanentPath,
                thumbnailUri = thumbPath,
                confirmationCode = confirmationCode,
                notes = notes,
                ocrRawText = ocrRawText,
                qrContent = qrContent
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

            // --- TRY AI FIRST, FALL BACK TO REGEX ---
            val aiResult = categorizeWithAI(rawText)

            if (aiResult != null && aiResult.suggestedTitle.isNotBlank()) {
                // AI succeeded — use its results
                aiResult
            } else {
                // AI failed (offline/error) — use regex heuristics as fallback

                // --- TITLE: Find the largest text block by bounding box area ---
                val suggestedTitle = visionText.textBlocks
                    .filter { block ->
                        val text = block.text.trim()
                        text.length > 3
                                && !text.matches(Regex("^\\d{1,2}[.:]+\\d{2}.*"))
                                && !text.matches(Regex("^\\d+$"))
                                && !text.matches(Regex("^[\\d\\s%°.:]+$"))
                    }
                    .maxByOrNull { block ->
                        val box = block.boundingBox
                        if (box != null) box.width() * box.height() else 0
                    }
                    ?.text
                    ?.lines()
                    ?.take(2)
                    ?.joinToString(" ")
                    ?.trim()
                    ?: ""

                // --- CONFIRMATION CODE: Look near label keywords ---
                val labelKeywords = listOf("order number", "booking", "confirmation", "conf", "reference", "ticket")

                var suggestedConfCode = ""
                for (block in visionText.textBlocks) {
                    val blockLower = block.text.lowercase()
                    if (labelKeywords.any { keyword -> blockLower.contains(keyword) }) {
                        val codeRegex = Regex("[A-Z0-9]{5,15}")
                        val match = codeRegex.find(block.text.uppercase())
                        if (match != null) {
                            suggestedConfCode = match.value
                            break
                        }
                    }
                }
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
                    Regex("([A-Za-z]{3,9})\\s+(\\d{1,2}),?\\s+(\\d{4})") to "MMM dd yyyy",
                    Regex("(\\d{1,2})\\s+([A-Za-z]{3,9})\\s+(\\d{4})") to "dd MMM yyyy",
                    Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})") to "yyyy-MM-dd",
                    Regex("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})") to "dd/MM/yyyy"
                )

                var suggestedDate: Long? = null
                for ((regex, pattern) in datePatterns) {
                    val match = regex.find(rawText)
                    if (match != null) {
                        try {
                            val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
                            suggestedDate = formatter.parse(
                                match.value.replace(",", "").replace("/", "-").let { raw ->
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult()
        } finally {
            _isScanning.value = false
        }
    }

    // ---- AI Categorization ----

    private val vertexApiKey = BuildConfig.VERTEX_API_KEY
    private val aiEndpoint = "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash:generateContent?key=$vertexApiKey"

    /**
     * Sends OCR text to Gemini and gets structured ticket data back.
     * Falls back to null if the API call fails (offline, quota, etc.)
     */
    private suspend fun categorizeWithAI(ocrText: String): OcrResult? = withContext(Dispatchers.IO) {
        if (ocrText.isBlank() || vertexApiKey.isBlank()) return@withContext null

        try {
            val prompt = """
You are a ticket data extractor. Analyze the following OCR text from a travel/event ticket and extract structured information.

OCR Text:
\"\"\"
$ocrText
\"\"\"

Respond ONLY with a JSON object (no markdown, no code fences, no explanation):
{
  "title": "the event name, flight info, or booking name (be descriptive)",
  "type": "one of: Flight, Hotel, Event, Train, Bus, Other",
  "date": "YYYY-MM-DD format or null if not found",
  "confirmationCode": "booking/order/confirmation number or null if not found"
}
""".trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val url = URL(aiEndpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseString)
                val aiText = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Clean up: remove markdown code fences if Gemini adds them
                val cleanJson = aiText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val parsed = JSONObject(cleanJson)

                // Parse the date
                var suggestedDate: Long? = null
                val dateStr = parsed.optString("date", "")
                if (dateStr.isNotBlank() && dateStr != "null") {
                    try {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
                        suggestedDate = formatter.parse(dateStr)?.time
                    } catch (_: Exception) { }
                }

                OcrResult(
                    rawText = ocrText,
                    suggestedTitle = parsed.optString("title", ""),
                    suggestedConfirmationCode = parsed.optString("confirmationCode", "").let {
                        if (it == "null") "" else it
                    },
                    suggestedDate = suggestedDate,
                    suggestedType = parsed.optString("type", "")   // ← ADD THIS
                )
            } else {
                null // API error — fall back to regex
            }
        } catch (e: Exception) {
            Log.e("TicketVM", "AI categorization failed: ${e.message}")
            null // Network error — fall back to regex
        }
    }

    // ---- QR/Barcode scanning ----

    /**
     * Scans the image for QR codes and barcodes.
     * Returns the raw content string of the first detected code, or null if none found.
     * Uses Bitmap decoding to handle both content:// and file:// URIs reliably.
     */
    suspend fun scanBarcode(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Decode the URI to a Bitmap — works with both content:// and file:// URIs
            val bitmap = appContext.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()

            val barcodes = suspendCancellableCoroutine { continuation ->
                scanner.process(image)
                    .addOnSuccessListener { results ->
                        continuation.resumeWith(Result.success(results))
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            bitmap.recycle()

            // Return the raw value of the first detected barcode/QR
            barcodes.firstOrNull()?.rawValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}