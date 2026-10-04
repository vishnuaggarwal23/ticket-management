package com.ticketmanagement.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-RAG-ING-04 — PDF metadata keys on every chunk (see {@code spec/rag-ingestion.md} §4.3).
 */
public final class ChunkMetadataAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChunkMetadataAssertions() {
    }

    public static void assertPdfAndTechnicalMetadataKeys(String metadataJson, String expectedTicketId) {
        JsonNode node = parse(metadataJson);
        assertThat(node.get("ticketId").asText()).isEqualTo(expectedTicketId);
        assertThat(node.has("status")).isTrue();
        assertThat(node.get("status").isNull()).isFalse();
        assertThat(node.has("priority")).isTrue();
        assertThat(node.get("priority").isNull()).isFalse();
        assertThat(node.has("assignee")).isTrue();
        assertThat(node.has("category")).isTrue();
        assertThat(node.has("chunkIndex")).isTrue();
        assertThat(node.has("ingestedAt")).isTrue();
        assertThat(node.get("ingestedAt").isNull()).isFalse();
    }

    private static JsonNode parse(String metadataJson) {
        try {
            return MAPPER.readTree(metadataJson);
        } catch (Exception ex) {
            throw new AssertionError("metadata is not valid JSON: " + metadataJson, ex);
        }
    }
}
