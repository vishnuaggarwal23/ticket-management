package com.ticketmanagement.service;

import com.ticketmanagement.config.RagProperties;
import com.ticketmanagement.rag.AskQuestionTicketIds;
import com.ticketmanagement.rag.EmbeddingPort;
import com.ticketmanagement.rag.GenerationPort;
import com.ticketmanagement.rag.VectorChunkStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AskService {

    public static final String NO_MATCH_ANSWER = "No relevant tickets found.";

    private final EmbeddingPort embeddings;
    private final VectorChunkStore store;
    private final GenerationPort generation;
    private final RagProperties ragProperties;

    public AskService(
            EmbeddingPort embeddings,
            VectorChunkStore store,
            GenerationPort generation,
            RagProperties ragProperties
    ) {
        this.embeddings = embeddings;
        this.store = store;
        this.generation = generation;
        this.ragProperties = ragProperties;
    }

    public AskResult ask(String question) {
        List<float[]> queryVectors = embeddings.embedAll(List.of(question));
        if (queryVectors.size() != 1) {
            throw new IllegalStateException("Expected one query embedding");
        }
        RagProperties.Retrieval retrieval = ragProperties.retrieval();
        List<VectorChunkStore.RetrievedChunk> vectorHits = store.searchSimilar(
                queryVectors.getFirst(),
                retrieval.topK(),
                retrieval.similarityThreshold()
        );
        List<VectorChunkStore.RetrievedChunk> hits = mergeRetrieval(question, vectorHits);
        if (hits.isEmpty()) {
            return new AskResult(NO_MATCH_ANSWER, List.of());
        }
        List<String> citedTicketIds = dedupeInOrder(hits);
        String answer = generation.generate(question, hits);
        return new AskResult(answer, citedTicketIds);
    }

    private List<VectorChunkStore.RetrievedChunk> mergeRetrieval(
            String question,
            List<VectorChunkStore.RetrievedChunk> vectorHits
    ) {
        List<VectorChunkStore.RetrievedChunk> merged = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String ticketId : AskQuestionTicketIds.extractInOrder(question)) {
            for (VectorChunkStore.StoredChunk stored : store.findByTicketId(ticketId)) {
                VectorChunkStore.RetrievedChunk chunk = new VectorChunkStore.RetrievedChunk(
                        stored.ticketId(),
                        stored.content(),
                        1.0
                );
                if (seen.add(chunkKey(chunk))) {
                    merged.add(chunk);
                }
            }
        }
        for (VectorChunkStore.RetrievedChunk hit : vectorHits) {
            if (seen.add(chunkKey(hit))) {
                merged.add(hit);
            }
        }
        return merged;
    }

    private static String chunkKey(VectorChunkStore.RetrievedChunk chunk) {
        return chunk.ticketId() + '\0' + chunk.content();
    }

    private static List<String> dedupeInOrder(List<VectorChunkStore.RetrievedChunk> hits) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (VectorChunkStore.RetrievedChunk hit : hits) {
            ids.add(hit.ticketId());
        }
        return new ArrayList<>(ids);
    }

    public record AskResult(String answer, List<String> citedTicketIds) {
    }
}
