package com.ticketmanagement.rag;

import java.util.List;

public interface EmbeddingPort {

    List<float[]> embedAll(List<String> texts);
}
