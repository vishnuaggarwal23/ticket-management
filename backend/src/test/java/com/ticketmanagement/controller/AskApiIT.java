package com.ticketmanagement.controller;

import com.ticketmanagement.dto.request.CreateTicketRequest;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.service.AskService;
import com.ticketmanagement.service.TicketService;
import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AskApiIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketService tickets;

    @ParameterizedTest
    @ValueSource(strings = {"/api/ai/ask", "/api/v1/ai/ask"})
    void groundedAskCitesRetrievedTicket(String path) throws Exception {
        String phrase = "zxq-payment-gateway-timeout-unique-" + path.replace("/", "");
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Payment", phrase, null, null, null));

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + phrase + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("Stubbed answer from retrieved tickets."))
                .andExpect(jsonPath("$.data.citedTicketIds", hasItem(created.id())))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void unmatchedQuestionIsNoMatchAndDoesNotCreateTickets() throws Exception {
        long before = tickets.list(0, 20, "createdAt,desc", null, null).meta().totalElements();

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"capital of francezzzz-no-ticket-match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(AskService.NO_MATCH_ANSWER))
                .andExpect(jsonPath("$.data.citedTicketIds").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());

        long after = tickets.list(0, 20, "createdAt,desc", null, null).meta().totalElements();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void blankQuestionIs400ValidationError() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void malformedAskJsonIs400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void unknownAskPropertyIs400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hello\",\"confidence\":0.9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void missingQuestionPropertyIs400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void questionAtMaxLengthIsAccepted() throws Exception {
        String question = "q".repeat(2000);
        mockMvc.perform(post("/api/v1/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").exists());
    }
}
