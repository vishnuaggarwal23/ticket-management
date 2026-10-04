package com.ticketmanagement.dto.serde;

import com.ticketmanagement.domain.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketPriorityJsonDeserializerTest {

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TicketPriority.class, new TicketPriorityJsonDeserializer());
        mapper = JsonMapper.builder().addModule(module).build();
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
