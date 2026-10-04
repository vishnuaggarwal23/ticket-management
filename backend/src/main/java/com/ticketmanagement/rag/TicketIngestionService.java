package com.ticketmanagement.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.persistence.TicketEntity;
import com.ticketmanagement.persistence.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TicketIngestionService.class);

    private final TicketRepository tickets;
    private final KnowledgeDocumentBuilder documents;
    private final TicketChunker chunker;
    private final EmbeddingPort embeddings;
    private final VectorChunkStore store;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public TicketIngestionService(
            TicketRepository tickets,
            KnowledgeDocumentBuilder documents,
            TicketChunker chunker,
            EmbeddingPort embeddings,
            VectorChunkStore store,
            RagProperties ragProperties,
            ObjectMapper objectMapper
    ) {
        this.tickets = tickets;
        this.documents = documents;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.store = store;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    public void ingest(String ticketId) {
        TicketEntity ticket = tickets.findWithCommentsById(ticketId).orElse(null);
        if (ticket == null) {
            log.warn("Skip ingest; ticket not found ticketId={}", ticketId);
            return;
        }
        KnowledgeDocument document = documents.build(ticket);
        if (!document.embeddable()) {
            store.replaceAll(ticketId, List.of());
            return;
        }
        List<TextChunk> chunks = chunker.chunk(document);
        if (chunks.isEmpty()) {
            store.replaceAll(ticketId, List.of());
            return;
        }
        List<float[]> vectors = embeddings.embedAll(chunks.stream().map(TextChunk::text).toList());
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch for ticket " + ticketId);
        }
        int dimensions = ragProperties.embedding().dimensions();
        List<VectorChunkStore.StoredChunk> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            float[] vector = vectors.get(i);
            if (vector.length != dimensions) {
                throw new IllegalStateException("Expected embedding dimension " + dimensions);
            }
            TextChunk chunk = chunks.get(i);
            rows.add(new VectorChunkStore.StoredChunk(
                    UUID.randomUUID(),
                    ticketId,
                    chunk.index(),
                    chunk.text(),
                    vector,
                    metadataJson(chunk.metadata()),
                    chunk.metadata().ingestedAt()
            ));
        }
        store.replaceAll(ticketId, rows);
    }

    private String metadataJson(RagChunkMetadata metadata) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("ticketId", metadata.ticketId());
        json.put("status", metadata.status() == null ? null : metadata.status().name());
        json.put("priority", metadata.priority() == null ? null : metadata.priority().name());
        json.put("assignee", metadata.assignee());
        json.put("category", metadata.category() == null ? null : metadata.category().name());
        json.put("chunkIndex", metadata.chunkIndex());
        json.put("ingestedAt", metadata.ingestedAt() == null ? null : metadata.ingestedAt().toString());
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize chunk metadata", ex);
        }
    }
}
