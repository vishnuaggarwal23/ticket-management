package com.ticketmanagement.api.ticket;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketConstraints;
import com.ticketmanagement.domain.TicketPriority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 1, max = TicketConstraints.TITLE_MAX) String title,
        @Size(max = TicketConstraints.DESCRIPTION_MAX) String description,
        TicketPriority priority,
        @Size(max = TicketConstraints.ASSIGNEE_MAX) String assignee,
        TicketCategory category,
        @Size(max = TicketConstraints.RESOLUTION_NOTES_MAX) String resolutionNotes
) {

    public boolean hasUpdates() {
        return title != null
                || description != null
                || priority != null
                || assignee != null
                || category != null
                || resolutionNotes != null;
    }
}
