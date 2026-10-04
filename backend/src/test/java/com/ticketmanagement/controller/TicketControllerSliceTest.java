package com.ticketmanagement.controller;

import com.ticketmanagement.advice.RestExceptionHandler;
import com.ticketmanagement.dto.common.PageMeta;
import com.ticketmanagement.dto.common.PageResponse;
import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.config.JacksonConfig;
import com.ticketmanagement.dto.request.CreateCommentRequest;
import com.ticketmanagement.dto.request.CreateTicketRequest;
import com.ticketmanagement.dto.request.UpdateTicketRequest;
import com.ticketmanagement.dto.response.CommentResponse;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.dto.response.TicketSummaryResponse;
import com.ticketmanagement.util.TicketConstraints;
import com.ticketmanagement.exception.TicketNotFoundException;
import com.ticketmanagement.exception.IllegalTicketTransitionException;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.service.TicketService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({RestExceptionHandler.class, JacksonConfig.class})
@EnableConfigurationProperties(ApiProperties.class)
@TestPropertySource(properties = {
        "app.api.page-size-default=20",
        "app.api.page-size-max=100"
})
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
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void createTitleAtMaxLengthIs201() throws Exception {
        when(tickets.create(any())).thenReturn(detail("TKT-1001", TicketPriority.MEDIUM));
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(TicketConstraints.TITLE_MAX) + "\"}"))
                .andExpect(status().isCreated());
        verify(tickets).create(any());
    }

    @Test
    void createTitleOverMaxLengthIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(TicketConstraints.TITLE_MAX + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).create(any());
    }

    @Test
    void createMissingTitleIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"only desc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).create(any());
    }

    @Test
    void createDescriptionOverMaxLengthIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Help\",\"description\":\""
                                + "d".repeat(TicketConstraints.DESCRIPTION_MAX + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).create(any());
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
    void patchTitleOverMaxLengthIs400() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(TicketConstraints.TITLE_MAX + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).updateFields(any(), any());
    }

    @Test
    void patchResolutionNotesOverMaxLengthIs400() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\""
                                + "n".repeat(TicketConstraints.RESOLUTION_NOTES_MAX + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).updateFields(any(), any());
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
        verify(tickets).list(0, 20, null, null, null);
    }

    @Test
    void unexpectedExceptionIs500WithoutLeak() throws Exception {
        when(tickets.getById("TKT-1001")).thenThrow(new IllegalStateException("SQL boom secret"));

        mockMvc.perform(get("/api/v1/tickets/TKT-1001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("SQL boom secret"))));
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
    void negativePageIs400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).list(anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void sizeOneAndOneHundredAreAccepted() throws Exception {
        when(tickets.list(0, 1, null, null, null)).thenReturn(new PageResponse<>(
                List.of(), new PageMeta(0, 1, 0, 0, "createdAt,desc")));
        when(tickets.list(0, 100, null, null, null)).thenReturn(new PageResponse<>(
                List.of(), new PageMeta(0, 100, 0, 0, "createdAt,desc")));

        mockMvc.perform(get("/api/v1/tickets").param("size", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tickets").param("size", "100")).andExpect(status().isOk());
        verify(tickets).list(0, 1, null, null, null);
        verify(tickets).list(0, 100, null, null, null);
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
        String tooLong = "a".repeat(TicketConstraints.ASSIGNEE_MAX + 1);
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
                .thenThrow(new com.ticketmanagement.exception.InvalidSortException("title"));

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

    @Test
    void blankCommentBodyIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/TKT-1001/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("body"));
        verify(tickets, never()).addComment(any(), any());
    }

    @Test
    void commentBodyOverMaxLengthIs400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/TKT-1001/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "c".repeat(TicketConstraints.COMMENT_BODY_MAX + 1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).addComment(any(), any());
    }

    @Test
    void illegalTransitionIs409() throws Exception {
        when(tickets.updateFields(eq("TKT-1001"), any()))
                .thenThrow(new IllegalTicketTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN));

        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.error.status").value(409))
                .andExpect(jsonPath("$.error.message").value("Cannot transition from CLOSED to OPEN"));
    }

    @Test
    void unknownPatchStatusIs400() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(tickets, never()).updateFields(any(), any());
    }

    @Test
    void legalTransitionReturns200Envelope() throws Exception {
        when(tickets.updateFields(eq("TKT-1001"), any()))
                .thenReturn(detail("TKT-1001", TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/v1/tickets/TKT-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    private static TicketDetailResponse detail(String id, TicketPriority priority) {
        return detail(id, priority, TicketStatus.OPEN);
    }

    private static TicketDetailResponse detail(String id, TicketPriority priority, TicketStatus status) {
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        return new TicketDetailResponse(
                id,
                "Help",
                "",
                status,
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
