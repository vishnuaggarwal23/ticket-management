package com.ticketmanagement.rag;

import com.ticketmanagement.util.TicketId;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.TicketEntity;
import com.ticketmanagement.repository.TicketRepository;
import com.ticketmanagement.service.TicketIngestionService;
import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import com.ticketmanagement.support.ChunkMetadataAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TicketIngestionIT.EmbedConfig.class)
class TicketIngestionIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TicketIngestionService ingestion;

    @Autowired
    private VectorChunkStore store;

    @Test
    void ingestedChunksExposeFullMetadataJson() {
        TicketEntity ticket = persist("metadata matrix billing help");
        ticket.setAssignee("billing@example.com");
        ticket.setCategory(com.ticketmanagement.domain.TicketCategory.BILLING);
        tickets.saveAndFlush(ticket);

        ingestion.ingest(ticket.getId());

        store.findByTicketId(ticket.getId()).forEach(chunk ->
                ChunkMetadataAssertions.assertPdfAndTechnicalMetadataKeys(chunk.metadataJson(), ticket.getId()));
    }

    @Test
    void ingestThenReIngestReplacesRows() {
        TicketEntity first = persist("need help with billing invoice");
        ingestion.ingest(first.getId());
        List<VectorChunkStore.StoredChunk> afterFirst = store.findByTicketId(first.getId());
        assertThat(afterFirst).isNotEmpty();
        assertThat(afterFirst.getFirst().embedding()).hasSize(768);
        assertThat(afterFirst.getFirst().metadataJson()).contains("\"status\":\"OPEN\"");
        int firstCount = afterFirst.size();

        first.setDescription("need help with billing invoice plus extra paragraph after blank line\n\nsecond paragraph here");
        tickets.save(first);
        ingestion.ingest(first.getId());
        List<VectorChunkStore.StoredChunk> afterSecond = store.findByTicketId(first.getId());
        assertThat(afterSecond).isNotEmpty();
        assertThat(afterSecond.stream().map(VectorChunkStore.StoredChunk::id).toList())
                .doesNotContainAnyElementsOf(afterFirst.stream().map(VectorChunkStore.StoredChunk::id).toList());
        assertThat(afterSecond.size()).isGreaterThanOrEqualTo(firstCount);
    }

    @Test
    void emptyTicketLeavesNoChunkRows() {
        TicketEntity empty = persist("");
        ingestion.ingest(empty.getId());
        assertThat(store.findByTicketId(empty.getId())).isEmpty();
    }

    @Test
    void searchSimilarDropsOrthogonalHashVectors() {
        TicketEntity ticket = persist("zxq-cosine-threshold-unique-phrase");
        ingestion.ingest(ticket.getId());
        HashEmbeddingPort hashes = new HashEmbeddingPort(768);
        float[] matching = hashes.embedAll(List.of(store.findByTicketId(ticket.getId()).getFirst().content())).getFirst();
        float[] orthogonal = hashes.embedAll(List.of("completely-different-unrelated-query-zzz")).getFirst();

        assertThat(store.searchSimilar(matching, 8, 0.72)).isNotEmpty();
        assertThat(store.searchSimilar(orthogonal, 8, 0.72)).isEmpty();
    }

    private TicketEntity persist(String description) {
        TicketEntity entity = new TicketEntity();
        entity.setId(TicketId.format(tickets.nextTicketNumber()));
        entity.setTitle("Ingest IT");
        entity.setDescription(description);
        entity.setStatus(TicketStatus.OPEN);
        entity.setPriority(TicketPriority.MEDIUM);
        return tickets.save(entity);
    }

    @TestConfiguration
    static class EmbedConfig {
        @Bean
        @Primary
        EmbeddingPort embeddingPort() {
            return new HashEmbeddingPort(768);
        }
    }
}
