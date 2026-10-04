package com.ticketmanagement.api.ticket;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketConstraints;
import com.ticketmanagement.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = TicketConstraints.TITLE_MAX) String title,
        @Size(max = TicketConstraints.DESCRIPTION_MAX) String description,
        TicketPriority priority,
        @Size(max = TicketConstraints.ASSIGNEE_MAX) String assignee,
        TicketCategory category
) {
}
