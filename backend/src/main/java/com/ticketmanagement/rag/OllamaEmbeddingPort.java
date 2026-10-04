package com.ticketmanagement.rag;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

public class OllamaEmbeddingPort implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingPort(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (Embedding embedding : response.getResults()) {
            vectors.add(embedding.getOutput());
        }
        return vectors;
    }
}
