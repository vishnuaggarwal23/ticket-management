package com.ticketmanagement.config;

import com.ticketmanagement.rag.DisabledEmbeddingPort;
import com.ticketmanagement.rag.DisabledGenerationPort;
import com.ticketmanagement.rag.EmbeddingPort;
import com.ticketmanagement.rag.GenerationPort;
import com.ticketmanagement.rag.OllamaEmbeddingPort;
import com.ticketmanagement.rag.OllamaGenerationPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class OllamaAiConfig {

    @Bean
    @Lazy
    EmbeddingPort embeddingPort(ObjectProvider<EmbeddingModel> embeddingModel) {
        EmbeddingModel model = embeddingModel.getIfAvailable();
        return model != null ? new OllamaEmbeddingPort(model) : new DisabledEmbeddingPort();
    }

    @Bean
    @Lazy
    GenerationPort generationPort(ObjectProvider<ChatModel> chatModel) {
        ChatModel model = chatModel.getIfAvailable();
        return model != null ? new OllamaGenerationPort(model) : new DisabledGenerationPort();
    }
}
