package com.ticketmanagement.rag;

public record TextChunk(int index, String text, RagChunkMetadata metadata) {
}
