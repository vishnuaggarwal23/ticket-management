package com.ticketmanagement.controller;

import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketRestartIT extends AbstractPostgresIntegrationTest {

    private static final String TITLE = "restart-persist-ticket";
    private static final String COMMENT = "comment-must-survive-restart";

    private static String ticketId;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void createTicketAndComment() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + TITLE + "\",\"description\":\"survives process restart\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        ticketId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        assertThat(ticketId).startsWith("TKT-");

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + COMMENT + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    void afterFreshContextTicketAndCommentRemain() throws Exception {
        assertThat(ticketId).isNotBlank();

        mockMvc.perform(get("/api/v1/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId))
                .andExpect(jsonPath("$.data.title").value(TITLE))
                .andExpect(jsonPath("$.data.description").value("survives process restart"))
                .andExpect(jsonPath("$.data.comments[0].body").value(COMMENT));
    }
}
