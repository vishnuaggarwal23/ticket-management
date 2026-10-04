package com.ticketmanagement.api.ticket;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;

import java.time.Instant;

public record TicketSummaryResponse(
        String id,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        TicketCategory category,
        Instant createdAt,
        Instant updatedAt
) {
}
