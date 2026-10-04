package com.ticketmanagement.api.ticket;

import com.ticketmanagement.domain.TicketConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(max = TicketConstraints.COMMENT_BODY_MAX) String body
) {
}
