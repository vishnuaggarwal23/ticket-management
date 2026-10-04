package com.ticketmanagement.dto.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ticketmanagement.domain.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketPriorityJsonDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TicketPriority.class, new TicketPriorityJsonDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void mapsUrgentToCritical() throws Exception {
        TicketPriority priority = mapper.readValue("\"URGENT\"", TicketPriority.class);
        assertThat(priority).isEqualTo(TicketPriority.CRITICAL);
        assertThat(mapper.writeValueAsString(TicketPriority.CRITICAL)).isEqualTo("\"CRITICAL\"");
    }

    @Test
    void readsCanonicalCritical() throws Exception {
        assertThat(mapper.readValue("\"CRITICAL\"", TicketPriority.class)).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void rejectsUnknownPriority() {
        assertThatThrownBy(() -> mapper.readValue("\"P1\"", TicketPriority.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
