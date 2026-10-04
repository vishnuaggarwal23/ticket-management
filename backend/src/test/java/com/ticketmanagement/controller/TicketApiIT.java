package com.ticketmanagement.controller;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.domain.TicketStatusMachine;
import com.ticketmanagement.rag.VectorChunkStore;
import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Autowired
    private VectorChunkStore vectorStore;

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
    void blankTitleCreateDoesNotIncreaseVectorRowCount() throws Exception {
        int rowsBefore = vectorRowCount();
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(vectorRowCount()).isEqualTo(rowsBefore);
    }

    @Test
    void illegalTransition409LeavesVectorChunksUnchanged() throws Exception {
        String id = createOpen("vector-409-" + System.nanoTime());
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"embeddable text for vector store\"}"))
                .andExpect(status().isOk());
        List<UUID> idsBefore = vectorStore.findByTicketId(id).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();
        assertThat(idsBefore).isNotEmpty();

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ILLEGAL_TRANSITION"));

        List<UUID> idsAfter = vectorStore.findByTicketId(id).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();
        assertThat(idsAfter).containsExactlyElementsOf(idsBefore);
        assertThat(dbStatus(id)).isEqualTo("OPEN");
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

    @Test
    void lifecycleOpenThroughClosedThenReopenIs409() throws Exception {
        String id = createOpen("sm-lifecycle-flow-a");
        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
        patchStatus(id, TicketStatus.IN_PROGRESS);
        patchStatus(id, TicketStatus.RESOLVED);
        patchStatus(id, TicketStatus.CLOSED);
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ILLEGAL_TRANSITION"));
        assertThat(dbStatus(id)).isEqualTo("CLOSED");
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void fieldPatchAndCommentAllowedOnTerminalStatus(TicketStatus terminal) throws Exception {
        String id = ticketIn(terminal);
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"terminal-edit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("terminal-edit"))
                .andExpect(jsonPath("$.data.status").value(terminal.name()));

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"note on terminal\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(terminal.name()))
                .andExpect(jsonPath("$.data.comments[0].body").value("note on terminal"));
        assertThat(dbStatus(id)).isEqualTo(terminal.name());
    }

    @Test
    void listFilterMatchesStatusAfterTransition() throws Exception {
        String id = createOpen("sm-filter-in-progress");
        patchStatus(id, TicketStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "sm-filter-in-progress")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "sm-filter-in-progress")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void legalTransitionPersists(TicketStatus from, TicketStatus to) throws Exception {
        String id = ticketIn(from);
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(to.name()));
        assertThat(dbStatus(id)).isEqualTo(to.name());
    }

    @ParameterizedTest
    @MethodSource("illegalPairs")
    void illegalTransitionIs409AndRowUnchanged(TicketStatus from, TicketStatus to) throws Exception {
        String id = ticketIn(from);
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to.name() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.error.message").value("Cannot transition from " + from + " to " + to));
        assertThat(dbStatus(id)).isEqualTo(from.name());
    }

    @Test
    void illegalTransitionDoesNotApplyOtherFields() throws Exception {
        String id = createOpen("sm-no-partial");
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"title\":\"should-not-apply\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.title").value("sm-no-partial"));
    }

    @Test
    void t2MayIncludeResolutionNotes() throws Exception {
        String id = ticketIn(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"resolutionNotes\":\"fixed in prod\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionNotes").value("fixed in prod"));
    }

    @Test
    void patchWithoutStatusLeavesStatusUnchanged() throws Exception {
        String id = createOpen("sm-fields-only");
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"still-open\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("still-open"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void unknownPatchStatusIs400() throws Exception {
        String id = createOpen("sm-bad-enum");
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertThat(dbStatus(id)).isEqualTo("OPEN");
    }

    static Stream<Arguments> illegalPairs() {
        TicketStatusMachine machine = new TicketStatusMachine();
        List<Arguments> pairs = new ArrayList<>();
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (!machine.isTransitionAllowed(from, to)) {
                    pairs.add(Arguments.of(from, to));
                }
            }
        }
        return pairs.stream();
    }

    private String ticketIn(TicketStatus from) throws Exception {
        String id = createOpen("sm-" + from + "-" + System.nanoTime());
        switch (from) {
            case OPEN -> {
            }
            case IN_PROGRESS -> patchStatus(id, TicketStatus.IN_PROGRESS);
            case RESOLVED -> {
                patchStatus(id, TicketStatus.IN_PROGRESS);
                patchStatus(id, TicketStatus.RESOLVED);
            }
            case CLOSED -> {
                patchStatus(id, TicketStatus.IN_PROGRESS);
                patchStatus(id, TicketStatus.RESOLVED);
                patchStatus(id, TicketStatus.CLOSED);
            }
            case CANCELLED -> patchStatus(id, TicketStatus.CANCELLED);
        }
        return id;
    }

    private void patchStatus(String id, TicketStatus to) throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(to.name()));
    }

    private String createOpen(String title) throws Exception {
        return json(mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn(), "$.data.id");
    }

    private String dbStatus(String id) {
        return jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, id);
    }

    private int vectorRowCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_vector_chunk", Integer.class);
        return count == null ? 0 : count;
    }

    private static String json(MvcResult result, String path) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
