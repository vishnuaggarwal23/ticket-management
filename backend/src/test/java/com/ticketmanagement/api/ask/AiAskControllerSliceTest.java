package com.ticketmanagement.api.ask;

import com.ticketmanagement.api.advice.RestExceptionHandler;
import com.ticketmanagement.config.JacksonConfig;
import com.ticketmanagement.rag.AskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAskController.class)
@Import({RestExceptionHandler.class, JacksonConfig.class})
class AiAskControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @ParameterizedTest
    @ValueSource(strings = {"/api/ai/ask", "/api/v1/ai/ask"})
    void groundedAskIs200OnBothPaths(String path) throws Exception {
        when(askService.ask("What caused previous payment failures?"))
                .thenReturn(new AskService.AskResult(
                        "Previous payment failures were linked to gateway timeouts.",
                        List.of("TKT-1001")));

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What caused previous payment failures?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("Previous payment failures were linked to gateway timeouts."))
                .andExpect(jsonPath("$.data.citedTicketIds[0]").value("TKT-1001"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void emptyRetrievalIs200NoMatch() throws Exception {
        when(askService.ask("nothing relevant")).thenReturn(
                new AskService.AskResult(AskService.NO_MATCH_ANSWER, List.of()));

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"nothing relevant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(AskService.NO_MATCH_ANSWER))
                .andExpect(jsonPath("$.data.citedTicketIds").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void blankQuestionIs400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(askService, never()).ask(any());
    }

    @Test
    void missingQuestionIs400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(askService, never()).ask(any());
    }

    @Test
    void questionOver2000Is400() throws Exception {
        String tooLong = "a".repeat(2001);
        mockMvc.perform(post("/api/v1/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(askService, never()).ask(any());
    }

    @Test
    void unknownPropertyIs400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hello\",\"ticketId\":\"TKT-1001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("ticketId"));
        verify(askService, never()).ask(any());
    }

    @Test
    void questionAtMaxLengthIsAccepted() throws Exception {
        String question = "a".repeat(2000);
        when(askService.ask(question)).thenReturn(
                new AskService.AskResult(AskService.NO_MATCH_ANSWER, List.of()));
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());
        verify(askService).ask(question);
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verify(askService, never()).ask(any());
    }
}
