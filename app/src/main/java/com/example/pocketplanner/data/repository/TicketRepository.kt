package com.example.pocketplanner.data.repository

import com.example.pocketplanner.data.local.dao.TicketDao
import com.example.pocketplanner.data.local.entity.TicketEntity
import javax.inject.Inject

class TicketRepository @Inject constructor(
    private val ticketDao: TicketDao
) {
    fun getAllTickets() = ticketDao.getAllTickets()
    fun getTicketsForTrip(tripId: String) = ticketDao.getTicketsForTrip(tripId)
    fun getUnlinkedTickets() = ticketDao.getUnlinkedTickets()
    fun getTicketById(id: String) = ticketDao.getTicketById(id)
    suspend fun addTicket(ticket: TicketEntity) = ticketDao.insertTicket(ticket)
    suspend fun deleteTicket(ticket: TicketEntity) = ticketDao.deleteTicket(ticket)
}