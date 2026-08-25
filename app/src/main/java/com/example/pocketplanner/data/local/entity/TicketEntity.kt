package com.example.pocketplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,            // UUID
    val userId: String = "", // ← ADD THIS: Firebase Auth UID for cloud sync
    val tripId: String? = null,            // Nullable — null = standalone, non-null = linked to a trip
    val title: String,                     // e.g. "Vietnam Airlines VN123"
    val type: String,                      // Flight, Hotel, Event, Train, Bus, Other
    val dateTime: Long,                    // Event/departure timestamp
    val imageUri: String,                  // Original full-quality image path (QR-safe, no re-encoding)
    val thumbnailUri: String? = null,      // Downsized preview for the list view
    val qrContent: String? = null,         // Raw text from QR/barcode as a backup
    val ocrRawText: String? = null,        // Full OCR text dump for searchability
    val confirmationCode: String? = null,  // Booking/confirmation code
    val notes: String = "",                // User notes
    val createdAt: Long = System.currentTimeMillis(),
    val isSyncedWithCloud: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)