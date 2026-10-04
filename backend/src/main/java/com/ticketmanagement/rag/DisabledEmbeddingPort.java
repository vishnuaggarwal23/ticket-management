package com.ticketmanagement.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(EmbeddingPort.class)
public class DisabledEmbeddingPort implements EmbeddingPort {

    @Override
    public List<float[]> embedAll(List<String> texts) {
        throw new IllegalStateException("Live embeddings are disabled; set a test or Ollama EmbeddingPort");
    }
}
