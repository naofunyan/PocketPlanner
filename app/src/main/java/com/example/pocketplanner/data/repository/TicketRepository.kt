package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.TicketDao
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.core.offline.SyncScheduler
import javax.inject.Inject

class TicketRepository @Inject constructor(
    private val ticketDao: TicketDao,
    private val syncScheduler: SyncScheduler
) {
    fun getAllTickets() = ticketDao.getAllTickets()
    fun getTicketsForTrip(tripId: String) = ticketDao.getTicketsForTrip(tripId)
    fun getUnlinkedTickets() = ticketDao.getUnlinkedTickets()
    fun getTicketById(id: String) = ticketDao.getTicketById(id)

    suspend fun addTicket(ticket: TicketEntity) {
        // Save locally first (offline-first)
        val newTicket = ticket.copy(isSyncedWithCloud = false, updatedAt = System.currentTimeMillis())
        ticketDao.insertTicket(newTicket)
        // Push to cloud in background
        syncScheduler.scheduleSync()
    }

    suspend fun deleteTicket(ticket: TicketEntity) {
        // Soft delete locally
        val deletedTicket = ticket.copy(
            isDeleted = true,
            isSyncedWithCloud = false,
            updatedAt = System.currentTimeMillis()
        )
        ticketDao.insertTicket(deletedTicket)
        // Push the tombstone to cloud in background
        syncScheduler.scheduleSync()
    }
}