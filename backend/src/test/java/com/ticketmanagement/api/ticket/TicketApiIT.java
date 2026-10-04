package com.ticketmanagement.api.ticket;

import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TicketApiIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPersistsAndGetReturnsOpen() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Payment failed\",\"description\":\"Card declined\",\"category\":\"PAYMENTS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andReturn();

        String id = json(created, "$.data.id");
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/v1/tickets/" + id);

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void validationDoesNotInsertRow() throws Exception {
        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket", Integer.class);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void listSearchFilterAndEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"unique-api-alpha\",\"description\":\"needle-word\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"unique-api-beta\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "needle-word")
                        .param("status", "OPEN")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("unique-api-alpha"))
                .andExpect(jsonPath("$.meta.size").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        mockMvc.perform(get("/api/v1/tickets").param("q", "no-such-ticket-zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test
    void patchFieldsAndCommentOrder() throws Exception {
        String id = json(mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"patch-me\",\"priority\":\"LOW\"}"))
                .andExpect(status().isCreated())
                .andReturn(), "$.data.id");

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "patched-title",
                                  "description": "patched-desc",
                                  "priority": "HIGH",
                                  "assignee": "desk-queue",
                                  "category": "BILLING",
                                  "resolutionNotes": "try reboot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("patched-title"))
                .andExpect(jsonPath("$.data.description").value("patched-desc"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.assignee").value("desk-queue"))
                .andExpect(jsonPath("$.data.category").value("BILLING"))
                .andExpect(jsonPath("$.data.resolutionNotes").value("try reboot"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"first comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("first comment"));

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"second comment\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].body").value("first comment"))
                .andExpect(jsonPath("$.data.comments[1].body").value("second comment"));
    }

    @Test
    void unknownSortIs400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("sort", "title,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownIdIs404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/TKT-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void emptyPatchIs400() throws Exception {
        String id = json(mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"empty-patch\"}"))
                .andReturn(), "$.data.id");

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWithStatusPropertyIsRejectedAndWritesNoRow() throws Exception {
        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket", Integer.class);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"should-not-persist\",\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest());

        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void patchUnknownIdIs404() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/TKT-999998")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void commentOnMissingTicketIs404() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/TKT-999997/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"orphan comment\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void lastPageOfList() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"last-page-aaa\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"last-page-bbb\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "last-page-")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("last-page-bbb"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void createUrgentPersistsCritical() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"urgent-one\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    private static String json(MvcResult result, String path) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
