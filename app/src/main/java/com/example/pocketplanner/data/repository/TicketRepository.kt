package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.TicketDao
import com.example.pocketplanner.data.local.entity.TicketEntity
import com.example.pocketplanner.data.sync.FirestoreSyncManager
import javax.inject.Inject

class TicketRepository @Inject constructor(
    private val ticketDao: TicketDao,
    private val syncManager: FirestoreSyncManager
) {
    fun getAllTickets() = ticketDao.getAllTickets()
    fun getTicketsForTrip(tripId: String) = ticketDao.getTicketsForTrip(tripId)
    fun getUnlinkedTickets() = ticketDao.getUnlinkedTickets()
    fun getTicketById(id: String) = ticketDao.getTicketById(id)

    suspend fun addTicket(ticket: TicketEntity) {
        // Save locally first (offline-first)
        ticketDao.insertTicket(ticket)
        // Push to cloud in background
        syncManager.pushTicketToCloud(ticket)
    }

    suspend fun deleteTicket(ticket: TicketEntity) {
        // Delete locally first
        ticketDao.deleteTicket(ticket)
        // Delete from cloud
        syncManager.deleteTicketFromCloud(ticket.id)
    }
}