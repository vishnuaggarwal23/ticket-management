package com.ticketmanagement.support;

import com.ticketmanagement.rag.EmbeddingPort;
import com.ticketmanagement.rag.GenerationPort;
import com.ticketmanagement.rag.HashEmbeddingPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRagDoublesConfig {

    @Bean
    @Primary
    EmbeddingPort testEmbeddingPort() {
        return new HashEmbeddingPort(768);
    }

    @Bean
    @Primary
    GenerationPort testGenerationPort() {
        return (question, context) -> "Stubbed answer from retrieved tickets.";
    }
}
