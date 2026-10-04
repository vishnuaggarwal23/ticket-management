package com.ticketmanagement.rag;

import tools.jackson.databind.json.JsonMapper;
import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.TicketEntity;
import com.ticketmanagement.repository.TicketRepository;
import com.ticketmanagement.service.TicketIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.support.ChunkMetadataAssertions;

@ExtendWith(MockitoExtension.class)
class TicketIngestionServiceTest {

    @Mock
    private TicketRepository tickets;
    @Mock
    private VectorChunkStore store;

    private HashEmbeddingPort embeddings;
    private TicketIngestionService service;

    @BeforeEach
    void setUp() {
        embeddings = new HashEmbeddingPort(768);
        RagProperties properties = new RagProperties(
                new RagProperties.Chunking("HYBRID_PARAGRAPH_THEN_FIXED", 800, 120, 80),
                new RagProperties.Retrieval(8, 0.72, "COSINE"),
                new RagProperties.Embedding("ollama", "nomic-embed-text", 768, "")
        );
        service = new TicketIngestionService(
                tickets,
                new KnowledgeDocumentBuilder(),
                new TicketChunker(properties),
                embeddings,
                store,
                properties,
                JsonMapper.builder().build()
        );
    }

    @Test
    void emptyContentDeletesRowsAndDoesNotEmbed() {
        TicketEntity ticket = ticket("TKT-1", "");
        when(tickets.findWithCommentsById("TKT-1")).thenReturn(Optional.of(ticket));

        service.ingest("TKT-1");

        assertThat(embeddings.calls()).isZero();
        verify(store).replaceAll("TKT-1", List.of());
    }

    @Test
    void descriptionIsChunkedEmbeddedAndStored() {
        TicketEntity ticket = ticket("TKT-1", "Need a refund for declined card");
        when(tickets.findWithCommentsById("TKT-1")).thenReturn(Optional.of(ticket));

        service.ingest("TKT-1");

        assertThat(embeddings.calls()).isEqualTo(1);
        ArgumentCaptor<List<VectorChunkStore.StoredChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(store).replaceAll(eq("TKT-1"), captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        assertThat(captor.getValue().getFirst().embedding()).hasSize(768);
        assertThat(captor.getValue().getFirst().metadataJson()).contains("\"ticketId\":\"TKT-1\"");
        assertThat(captor.getValue().getFirst().metadataJson()).contains("\"status\":\"OPEN\"");
        assertThat(captor.getValue().getFirst().metadataJson()).contains("\"priority\":\"MEDIUM\"");
        VectorChunkStore.StoredChunk stored = captor.getValue().getFirst();
        ChunkMetadataAssertions.assertPdfAndTechnicalMetadataKeys(stored.metadataJson(), "TKT-1");
        assertThat(stored.metadataJson()).contains("\"category\"");
        assertThat(stored.content()).contains("Need a refund");
    }

    @Test
    void everyStoredChunkHasFullMetadataSnapshot() {
        TicketEntity ticket = ticket("TKT-42", "Shipment delay on route 7");
        ticket.setAssignee("ops@example.com");
        ticket.setCategory(TicketCategory.SHIPMENT);
        ticket.setPriority(TicketPriority.CRITICAL);
        when(tickets.findWithCommentsById("TKT-42")).thenReturn(Optional.of(ticket));

        service.ingest("TKT-42");

        ArgumentCaptor<List<VectorChunkStore.StoredChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(store).replaceAll(eq("TKT-42"), captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        captor.getValue().forEach(chunk ->
                ChunkMetadataAssertions.assertPdfAndTechnicalMetadataKeys(chunk.metadataJson(), "TKT-42"));
    }

    @Test
    void resolutionNotesAreIngested() {
        TicketEntity ticket = ticket("TKT-1", "");
        ticket.setResolutionNotes("Issued refund after gateway timeout");
        when(tickets.findWithCommentsById("TKT-1")).thenReturn(Optional.of(ticket));

        service.ingest("TKT-1");

        ArgumentCaptor<List<VectorChunkStore.StoredChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(store).replaceAll(eq("TKT-1"), captor.capture());
        assertThat(captor.getValue().stream().map(VectorChunkStore.StoredChunk::content).toList())
                .anyMatch(content -> content.contains("Issued refund after gateway timeout"));
    }

    @Test
    void unknownTicketDoesNotTouchStore() {
        when(tickets.findWithCommentsById("TKT-missing")).thenReturn(Optional.empty());
        service.ingest("TKT-missing");
        verify(store, never()).replaceAll(any(), any());
        assertThat(embeddings.calls()).isZero();
    }

    private static TicketEntity ticket(String id, String description) {
        TicketEntity entity = new TicketEntity();
        entity.setId(id);
        entity.setTitle("Help");
        entity.setDescription(description);
        entity.setStatus(TicketStatus.OPEN);
        entity.setPriority(TicketPriority.MEDIUM);
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
