package com.ticketmanagement.rag;

import java.util.ArrayList;
import java.util.List;

public class HashEmbeddingPort implements EmbeddingPort {

    private final int dimensions;
    private int calls;

    public HashEmbeddingPort(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        calls++;
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            float[] vector = new float[dimensions];
            int slot = Math.floorMod(text.hashCode(), dimensions);
            vector[slot] = 1.0f;
            vectors.add(vector);
        }
        return vectors;
    }

    public int calls() {
        return calls;
    }
}
