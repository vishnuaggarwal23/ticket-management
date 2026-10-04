package com.ticketmanagement.rag;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;

import java.time.Instant;

public record RagChunkMetadata(
        String ticketId,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        TicketCategory category,
        int chunkIndex,
        Instant ingestedAt
) {
}
