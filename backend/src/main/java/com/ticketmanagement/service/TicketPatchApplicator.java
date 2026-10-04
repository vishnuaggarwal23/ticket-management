package com.ticketmanagement.service;

import com.ticketmanagement.domain.TicketStatusMachine;
import com.ticketmanagement.dto.request.UpdateTicketRequest;
import com.ticketmanagement.entity.TicketEntity;

/**
 * Applies non-status and status fields from a PATCH request onto a loaded ticket entity.
 */
final class TicketPatchApplicator {

    private final TicketStatusMachine statusMachine;

    TicketPatchApplicator(TicketStatusMachine statusMachine) {
        this.statusMachine = statusMachine;
    }

    void apply(TicketEntity ticket, UpdateTicketRequest request) {
        if (request.status() != null) {
            statusMachine.assertTransitionAllowed(ticket.getStatus(), request.status());
        }
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (request.resolutionNotes() != null) {
            ticket.setResolutionNotes(request.resolutionNotes());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
    }
}
