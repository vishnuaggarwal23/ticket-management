package com.ticketmanagement.api.ticket;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String body,
        Instant createdAt
) {
}
