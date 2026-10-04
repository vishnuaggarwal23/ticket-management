package com.ticketmanagement.api.ticket;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
        String id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        TicketCategory category,
        String resolutionNotes,
        List<CommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {
}
