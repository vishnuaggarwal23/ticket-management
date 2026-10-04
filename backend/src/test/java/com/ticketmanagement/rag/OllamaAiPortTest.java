package com.ticketmanagement.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaAiPortTest {

    @Mock
    private EmbeddingModel embeddingModel;
    @Mock
    private ChatModel chatModel;

    @Test
    void embeddingPortMapsModelOutput() {
        when(embeddingModel.embedForResponse(List.of("hello"))).thenReturn(
                new EmbeddingResponse(List.of(new Embedding(new float[]{0.25f, 0.75f}, 0))));

        List<float[]> vectors = new OllamaEmbeddingPort(embeddingModel).embedAll(List.of("hello"));

        assertThat(vectors).hasSize(1);
        assertThat(vectors.getFirst()).containsExactly(0.25f, 0.75f);
    }

    @Test
    void generationPortCallsChatWithRetrievedExcerpts() {
        ChatResponse response = new ChatResponse(List.of(
                new Generation(new AssistantMessage("Grounded from TKT-1001."))));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        String answer = new OllamaGenerationPort(chatModel).generate(
                "payment?",
                List.of(new VectorChunkStore.RetrievedChunk("TKT-1001", "gateway timeout", 0.9)));

        assertThat(answer).isEqualTo("Grounded from TKT-1001.");
        verify(chatModel).call(any(Prompt.class));
    }
}
