package com.ticketmanagement;

import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationSmokeTest extends AbstractPostgresIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of("ticket", "ticket_comment", "ticket_vector_chunk");

    private static final List<String> EXPECTED_INDEXES = List.of(
            "ticket_pkey",
            "idx_ticket_status",
            "idx_ticket_created_at",
            "idx_ticket_updated_at",
            "idx_ticket_priority",
            "idx_ticket_status_created_at",
            "idx_ticket_title_trgm",
            "idx_ticket_description_trgm",
            "ticket_comment_pkey",
            "idx_ticket_comment_ticket_created",
            "ticket_vector_chunk_pkey",
            "uq_ticket_vector_chunk_ticket_chunk",
            "idx_ticket_vector_chunk_embedding_hnsw"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndRelationalSchemaIsApplied() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN ('ticket', 'ticket_comment', 'ticket_vector_chunk')
                        """,
                String.class);
        assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);

        Long sequenceStart = jdbcTemplate.queryForObject(
                "SELECT start_value FROM pg_sequences WHERE sequencename = 'ticket_number_seq'",
                Long.class);
        assertThat(sequenceStart).isEqualTo(1001L);

        Set<String> indexes = new HashSet<>(jdbcTemplate.queryForList(
                """
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND tablename IN ('ticket', 'ticket_comment', 'ticket_vector_chunk')
                        """,
                String.class));
        assertThat(indexes).containsAll(EXPECTED_INDEXES);

        Integer vectorExt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class);
        assertThat(vectorExt).isEqualTo(1);
    }
}
