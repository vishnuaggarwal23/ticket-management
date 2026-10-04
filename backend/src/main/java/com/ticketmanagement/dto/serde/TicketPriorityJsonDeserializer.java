package com.ticketmanagement.dto.serde;

import com.ticketmanagement.domain.TicketPriority;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class TicketPriorityJsonDeserializer extends ValueDeserializer<TicketPriority> {

    @Override
    public TicketPriority deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
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
