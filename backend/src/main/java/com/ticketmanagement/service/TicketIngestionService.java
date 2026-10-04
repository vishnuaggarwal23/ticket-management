package com.ticketmanagement.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.entity.TicketEntity;
import com.ticketmanagement.rag.EmbeddingPort;
import com.ticketmanagement.rag.KnowledgeDocument;
import com.ticketmanagement.rag.KnowledgeDocumentBuilder;
import com.ticketmanagement.rag.RagChunkMetadata;
import com.ticketmanagement.rag.TextChunk;
import com.ticketmanagement.rag.TicketChunker;
import com.ticketmanagement.rag.VectorChunkStore;
import com.ticketmanagement.repository.TicketRepository;
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

    private final TicketRepository ticketRepository;
    private final KnowledgeDocumentBuilder documents;
    private final TicketChunker chunker;
    private final EmbeddingPort embeddings;
    private final VectorChunkStore store;
    private final RagProperties ragProperties;
    private final JsonMapper jsonMapper;

    public TicketIngestionService(
            TicketRepository ticketRepository,
            KnowledgeDocumentBuilder documents,
            TicketChunker chunker,
            EmbeddingPort embeddings,
            VectorChunkStore store,
            RagProperties ragProperties,
            JsonMapper jsonMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.documents = documents;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.store = store;
        this.ragProperties = ragProperties;
        this.jsonMapper = jsonMapper;
    }

    public void ingest(String ticketId) {
        TicketEntity ticket = ticketRepository.findWithCommentsById(ticketId).orElse(null);
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
        store.replaceAll(ticketId, toStoredChunks(ticketId, chunks, vectors));
    }

    private List<VectorChunkStore.StoredChunk> toStoredChunks(
            String ticketId,
            List<TextChunk> chunks,
            List<float[]> vectors
    ) {
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch for ticket " + ticketId);
        }
        int dimensions = ragProperties.embedding().dimensions();
        List<VectorChunkStore.StoredChunk> rows = new ArrayList<>(chunks.size());
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
                    serializeMetadata(chunk.metadata()),
                    chunk.metadata().ingestedAt()
            ));
        }
        return rows;
    }

    private String serializeMetadata(RagChunkMetadata metadata) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("ticketId", metadata.ticketId());
        json.put("status", metadata.status() == null ? null : metadata.status().name());
        json.put("priority", metadata.priority() == null ? null : metadata.priority().name());
        json.put("assignee", metadata.assignee());
        json.put("category", metadata.category() == null ? null : metadata.category().name());
        json.put("chunkIndex", metadata.chunkIndex());
        json.put("ingestedAt", metadata.ingestedAt() == null ? null : metadata.ingestedAt().toString());
        try {
            return jsonMapper.writeValueAsString(json);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize chunk metadata", ex);
        }
    }
}
