package com.ticketmanagement.rag;

import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.service.AskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskServiceTest {

    @Mock
    private EmbeddingPort embeddings;
    @Mock
    private VectorChunkStore store;
    @Mock
    private GenerationPort generation;

    private AskService service;

    @BeforeEach
    void setUp() {
        RagProperties properties = new RagProperties(
                new RagProperties.Chunking("HYBRID_PARAGRAPH_THEN_FIXED", 800, 120, 80),
                new RagProperties.Retrieval(8, 0.72, "COSINE"),
                new RagProperties.Embedding("ollama", "nomic-embed-text", 768, "")
        );
        service = new AskService(embeddings, store, generation, properties);
    }

    @Test
    void emptyRetrievalReturnsNoMatchWithoutGenerate() {
        when(embeddings.embedAll(List.of("unknown topic"))).thenReturn(List.of(new float[]{1f}));
        when(store.searchSimilar(any(), eq(8), eq(0.72))).thenReturn(List.of());

        AskService.AskResult result = service.ask("unknown topic");

        assertThat(result.answer()).isEqualTo(AskService.NO_MATCH_ANSWER);
        assertThat(result.citedTicketIds()).isEmpty();
        verify(generation, never()).generate(any(), any());
    }

    @Test
    void citationsFollowRetrievalOrderAndDedupe() {
        when(embeddings.embedAll(List.of("payment"))).thenReturn(List.of(new float[]{1f}));
        when(store.searchSimilar(any(), eq(8), eq(0.72))).thenReturn(List.of(
                new VectorChunkStore.RetrievedChunk("TKT-1004", "timeout", 0.95),
                new VectorChunkStore.RetrievedChunk("TKT-1001", "gateway", 0.90),
                new VectorChunkStore.RetrievedChunk("TKT-1004", "retry", 0.80)
        ));
        when(generation.generate(eq("payment"), any())).thenReturn("Payment timeouts were seen before.");

        AskService.AskResult result = service.ask("payment");

        assertThat(result.citedTicketIds()).containsExactly("TKT-1004", "TKT-1001");
        assertThat(result.answer()).isEqualTo("Payment timeouts were seen before.");
        verify(generation).generate(eq("payment"), any());
    }

    @Test
    void generateReceivesRetrievedExcerptsOnly() {
        List<VectorChunkStore.RetrievedChunk> hits = List.of(
                new VectorChunkStore.RetrievedChunk("TKT-1001", "gateway timeout on checkout", 0.91)
        );
        when(embeddings.embedAll(List.of("checkout"))).thenReturn(List.of(new float[]{1f}));
        when(store.searchSimilar(any(), eq(8), eq(0.72))).thenReturn(hits);
        when(generation.generate(eq("checkout"), eq(hits))).thenReturn("Timeouts on checkout.");

        AskService.AskResult result = service.ask("checkout");

        assertThat(result.citedTicketIds()).containsExactly("TKT-1001");
        verify(generation).generate("checkout", hits);
    }

    @Test
    void explicitTicketIdLoadsStoredChunksWhenVectorSearchIsEmpty() {
        String question = "What is the status of TKT-1006?";
        VectorChunkStore.StoredChunk stored = new VectorChunkStore.StoredChunk(
                UUID.randomUUID(),
                "TKT-1006",
                0,
                "Ticket TKT-1006: Form\nStatus: OPEN | Priority: CRITICAL",
                new float[768],
                "{}",
                Instant.parse("2026-10-04T00:00:00Z")
        );
        when(embeddings.embedAll(List.of(question))).thenReturn(List.of(new float[]{1f}));
        when(store.searchSimilar(any(), eq(8), eq(0.72))).thenReturn(List.of());
        when(store.findByTicketId("TKT-1006")).thenReturn(List.of(stored));
        when(generation.generate(eq(question), any())).thenReturn("Status is OPEN.");

        AskService.AskResult result = service.ask(question);

        assertThat(result.citedTicketIds()).containsExactly("TKT-1006");
        assertThat(result.answer()).isEqualTo("Status is OPEN.");
        verify(generation).generate(eq(question), any());
    }

    @Test
    void explicitTicketIdChunksPrecedeVectorHitsForCitationOrder() {
        String question = "Status of TKT-1006?";
        VectorChunkStore.StoredChunk stored = new VectorChunkStore.StoredChunk(
                UUID.randomUUID(),
                "TKT-1006",
                0,
                "header chunk",
                new float[768],
                "{}",
                Instant.parse("2026-10-04T00:00:00Z")
        );
        when(embeddings.embedAll(List.of(question))).thenReturn(List.of(new float[]{1f}));
        when(store.searchSimilar(any(), eq(8), eq(0.72))).thenReturn(List.of(
                new VectorChunkStore.RetrievedChunk("TKT-1001", "other", 0.95)
        ));
        when(store.findByTicketId("TKT-1006")).thenReturn(List.of(stored));
        when(generation.generate(eq(question), any())).thenReturn("ok");

        AskService.AskResult result = service.ask(question);

        assertThat(result.citedTicketIds()).containsExactly("TKT-1006", "TKT-1001");
    }
}
