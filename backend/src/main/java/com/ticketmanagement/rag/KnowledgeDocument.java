package com.ticketmanagement.rag;

import java.time.Instant;

public record KnowledgeDocument(
        String ticketId,
        String title,
        String assembledText,
        RagChunkMetadata metadataSnapshot,
        Instant assembledAt,
        boolean embeddable
) {
}
