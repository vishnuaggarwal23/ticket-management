package com.ticketmanagement.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketIdTest {

    @Test
    void formatsSequenceValue() {
        assertThat(TicketId.format(1001)).isEqualTo("TKT-1001");
    }
}
