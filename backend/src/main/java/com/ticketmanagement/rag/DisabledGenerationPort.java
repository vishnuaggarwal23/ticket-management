package com.ticketmanagement.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(GenerationPort.class)
public class DisabledGenerationPort implements GenerationPort {

    @Override
    public String generate(String question, List<VectorChunkStore.RetrievedChunk> context) {
        throw new IllegalStateException("Live generation is disabled; set a test or Ollama GenerationPort");
    }
}
