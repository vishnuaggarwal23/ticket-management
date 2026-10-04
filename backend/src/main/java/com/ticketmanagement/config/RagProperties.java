package com.ticketmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(Chunking chunking, Retrieval retrieval, Embedding embedding) {

    public record Chunking(String strategy, int maxChars, int minChars, int overlapChars) {
    }

    public record Retrieval(int topK, double similarityThreshold, String distanceMetric) {
    }

    public record Embedding(String provider, String model, int dimensions, String baseUrl) {
    }
}
