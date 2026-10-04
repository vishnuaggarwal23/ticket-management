package com.ticketmanagement.api.ticket;

import com.ticketmanagement.api.advice.RestExceptionHandler;
import com.ticketmanagement.api.common.PageMeta;
import com.ticketmanagement.api.common.PageResponse;
import com.ticketmanagement.config.JacksonConfig;
import com.ticketmanagement.domain.TicketNotFoundException;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({RestExceptionHandler.class, JacksonConfig.class})
class TicketControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService tickets;

    @Test
    void createReturns201AndLocation() throws Exception {
        when(tickets.create(any())).thenReturn(detail("TKT-1001", TicketPriority.MEDIUM));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Help\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/TKT-1001"))
                .andExpect(jsonPath("$.data.id").value("TKT-1001"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void createUrgentMapsToCritical() throws Exception {
        when(tickets.create(any())).thenReturn(detail("TKT-1001", TicketPriority.CRITICAL));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Help\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));

        ArgumentCaptor<CreateTicketRequest> captor = ArgumentCaptor.forClass(CreateTicketRequest.class);
        verify(tickets).create(captor.capture());
        assertThat(captor.getValue().priority()).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void patchUrgentMapsToCritical() throws Exception {
        when(tickets.updateFields(eq("TKT-1001"), any())).thenReturn(detail("TKT-1001", TicketPriority.CRITICAL));

        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    @Test
    void getUnknownReturns404Envelope() throws Exception {
        when(tickets.getById("TKT-404")).thenThrow(new TicketNotFoundException("TKT-404"));

        mockMvc.perform(get("/api/v1/tickets/TKT-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path").value("/api/v1/tickets/TKT-404"));
    }

    @Test
    void listReturnsMeta() throws Exception {
        when(tickets.list(0, 20, null, null, null)).thenReturn(new PageResponse<>(
                List.of(),
                new PageMeta(0, 20, 0, 0, "createdAt,desc")));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20));
    }

    @Test
    void sizeZeroIs400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void size101Is400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidStatusQueryIs400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void assigneeLongerThan320Is400() throws Exception {
        String tooLong = "a".repeat(321);
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Help\",\"assignee\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nonEmailAssigneeIsAccepted() throws Exception {
        when(tickets.create(any())).thenReturn(detail("TKT-1001", TicketPriority.MEDIUM));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Help\",\"assignee\":\"not-an-email\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateTicketRequest> captor = ArgumentCaptor.forClass(CreateTicketRequest.class);
        verify(tickets).create(captor.capture());
        assertThat(captor.getValue().assignee()).isEqualTo("not-an-email");
    }

    @Test
    void unknownSortIs400() throws Exception {
        when(tickets.list(0, 20, "title,desc", null, null))
                .thenThrow(new com.ticketmanagement.domain.InvalidSortException("title"));

        mockMvc.perform(get("/api/v1/tickets").param("sort", "title,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getReturns200() throws Exception {
        when(tickets.getById("TKT-1001")).thenReturn(detail("TKT-1001", TicketPriority.MEDIUM));
        mockMvc.perform(get("/api/v1/tickets/TKT-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("TKT-1001"));
    }

    private static TicketDetailResponse detail(String id, TicketPriority priority) {
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        return new TicketDetailResponse(
                id,
                "Help",
                "",
                TicketStatus.OPEN,
                priority,
                null,
                null,
                null,
                List.of(),
                now,
                now
        );
    }
}
