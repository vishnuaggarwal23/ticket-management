package com.ticketmanagement.rag;

import java.util.List;

/**
 * Fail-fast stub when no {@link EmbeddingModel} is available (see {@code OllamaAiConfig}).
 */
public class DisabledEmbeddingPort implements EmbeddingPort {

    @Override
    public List<float[]> embedAll(List<String> texts) {
        throw new IllegalStateException(
                "Embeddings are not configured. Start Ollama with nomic-embed-text or check spring.ai.ollama settings.");
    }
}
