package com.ticketmanagement.config;

import com.ticketmanagement.rag.EmbeddingPort;
import com.ticketmanagement.rag.GenerationPort;
import com.ticketmanagement.rag.OllamaEmbeddingPort;
import com.ticketmanagement.rag.OllamaGenerationPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaAiConfig {

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    EmbeddingPort ollamaEmbeddingPort(EmbeddingModel embeddingModel) {
        return new OllamaEmbeddingPort(embeddingModel);
    }

    @Bean
    @ConditionalOnBean(ChatModel.class)
    GenerationPort ollamaGenerationPort(ChatModel chatModel) {
        return new OllamaGenerationPort(chatModel);
    }
}
