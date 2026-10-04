package com.ticketmanagement.rag;

import java.util.List;

public interface GenerationPort {

    String generate(String question, List<VectorChunkStore.RetrievedChunk> context);
}
