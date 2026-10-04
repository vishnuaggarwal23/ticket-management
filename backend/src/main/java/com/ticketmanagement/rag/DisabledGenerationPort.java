package com.ticketmanagement.rag;

import java.util.List;

/**
 * Fail-fast stub when no {@link org.springframework.ai.chat.model.ChatModel} is available (see {@code OllamaAiConfig}).
 */
public class DisabledGenerationPort implements GenerationPort {

    @Override
    public String generate(String question, List<VectorChunkStore.RetrievedChunk> context) {
        throw new IllegalStateException(
                "Chat generation is not configured. Start Ollama with your chat model or check spring.ai.ollama settings.");
    }
}
