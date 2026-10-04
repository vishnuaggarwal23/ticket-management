package com.ticketmanagement.dto.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.ticketmanagement.domain.TicketPriority;

import java.io.IOException;

public class TicketPriorityJsonDeserializer extends JsonDeserializer<TicketPriority> {

    @Override
    public TicketPriority deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if ("URGENT".equals(raw)) {
            return TicketPriority.CRITICAL;
        }
        try {
            return TicketPriority.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return (TicketPriority) context.handleWeirdStringValue(
                    TicketPriority.class, raw, "Unknown ticket priority");
        }
    }
}
