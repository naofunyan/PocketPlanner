package com.example.pocketplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketplanner.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    // Get ALL tickets, sorted newest first
    @Query("SELECT * FROM tickets ORDER BY dateTime DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    // Get tickets linked to a specific trip
    @Query("SELECT * FROM tickets WHERE tripId = :tripId ORDER BY dateTime DESC")
    fun getTicketsForTrip(tripId: String): Flow<List<TicketEntity>>

    // Get standalone tickets (not linked to any trip)
    @Query("SELECT * FROM tickets WHERE tripId IS NULL ORDER BY dateTime DESC")
    fun getUnlinkedTickets(): Flow<List<TicketEntity>>

    // Get a single ticket by ID
    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    fun getTicketById(id: String): Flow<TicketEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Delete
    suspend fun deleteTicket(ticket: TicketEntity)
}