package com.ticketmanagement.rag;

import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketChunkerTest {

    private final TicketChunker chunker = new TicketChunker(rag(800, 120, 80));

    @Test
    void emptySkipWhenNotEmbeddable() {
        KnowledgeDocument document = document(false, "Ticket TKT-1: t\n\nDescription:\n\nComments:\n(none)\n\nResolution:\n(none)");
        assertThat(chunker.chunk(document)).isEmpty();
    }

    @Test
    void commentBoundariesAreSeparateChunks() {
        String assembled = """
                Ticket TKT-1: Pay
                Status: OPEN | Priority: HIGH | Assignee: a | Category: PAYMENTS

                Description:
                Card failed.

                Comments:
                - [2026-01-02T10:00:00Z] first note
                - [2026-01-02T14:30:00Z] second note

                Resolution:
                (none)
                """;
        List<TextChunk> chunks = chunker.chunk(document(true, assembled));
        assertThat(chunks.stream().map(TextChunk::text))
                .contains("Card failed.", "- [2026-01-02T10:00:00Z] first note", "- [2026-01-02T14:30:00Z] second note");
        assertThat(chunks.stream().filter(c -> c.text().contains("first note") && c.text().contains("second note")))
                .isEmpty();
    }

    @Test
    void overflowSplitsLongDescription() {
        TicketChunker tight = new TicketChunker(rag(20, 5, 4));
        String assembled = """
                Ticket TKT-1: Pay
                Status: OPEN | Priority: HIGH | Assignee: a | Category: PAYMENTS

                Description:
                abcdefghijklmnopqrstuvwxyz

                Comments:
                (none)

                Resolution:
                (none)
                """;
        List<TextChunk> chunks = tight.chunk(document(true, assembled));
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.getFirst().text().length()).isLessThanOrEqualTo(20);
    }

    @Test
    void splitOverflowUsesOverlap() {
        List<String> parts = TicketChunker.splitOverflow("abcdefghij", 6, 2);
        assertThat(parts).containsExactly("abcdef", "efghij");
    }

    private static KnowledgeDocument document(boolean embeddable, String assembled) {
        RagChunkMetadata meta = new RagChunkMetadata(
                "TKT-1", TicketStatus.OPEN, TicketPriority.HIGH, "a", null, 0, Instant.parse("2026-10-04T00:00:00Z"));
        return new KnowledgeDocument("TKT-1", "Pay", assembled, meta, Instant.parse("2026-10-04T00:00:00Z"), embeddable);
    }

    private static RagProperties rag(int max, int min, int overlap) {
        return new RagProperties(
                new RagProperties.Chunking("HYBRID_PARAGRAPH_THEN_FIXED", max, min, overlap),
                new RagProperties.Retrieval(8, 0.72, "COSINE"),
                new RagProperties.Embedding("ollama", "nomic-embed-text", 768, "")
        );
    }
}
