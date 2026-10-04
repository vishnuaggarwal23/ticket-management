package com.ticketmanagement.rag;

import com.ticketmanagement.config.RagProperties;
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
        List<VectorChunkStore.RetrievedChunk> hits = store.searchSimilar(
                queryVectors.getFirst(),
                retrieval.topK(),
                retrieval.similarityThreshold()
        );
        if (hits.isEmpty()) {
            return new AskResult(NO_MATCH_ANSWER, List.of());
        }
        List<String> citedTicketIds = dedupeInOrder(hits);
        String answer = generation.generate(question, hits);
        return new AskResult(answer, citedTicketIds);
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
