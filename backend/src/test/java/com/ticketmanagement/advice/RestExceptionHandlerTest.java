package com.ticketmanagement.advice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    @Test
    void ticketIdFromPath() {
        assertThat(RestExceptionHandler.ticketIdFrom("/api/v1/tickets/TKT-1001")).isEqualTo("TKT-1001");
        assertThat(RestExceptionHandler.ticketIdFrom("/api/v1/tickets/TKT-1001/comments")).isEqualTo("TKT-1001");
        assertThat(RestExceptionHandler.ticketIdFrom("/api/v1/tickets")).isNull();
        assertThat(RestExceptionHandler.ticketIdFrom(null)).isNull();
    }
}
