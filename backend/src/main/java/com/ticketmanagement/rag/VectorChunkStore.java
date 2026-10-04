package com.ticketmanagement.rag;

import java.util.List;
import java.util.UUID;

public interface VectorChunkStore {

    void replaceAll(String ticketId, List<StoredChunk> chunks);

    List<StoredChunk> findByTicketId(String ticketId);

    List<RetrievedChunk> searchSimilar(float[] query, int topK, double minSimilarity);

    record RetrievedChunk(String ticketId, String content, double similarity) {
    }

    record StoredChunk(
            UUID id,
            String ticketId,
            int chunkIndex,
            String content,
            float[] embedding,
            String metadataJson,
            java.time.Instant ingestedAt
    ) {
    }
}
