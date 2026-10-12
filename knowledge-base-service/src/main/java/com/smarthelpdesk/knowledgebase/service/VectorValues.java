package com.smarthelpdesk.knowledgebase.service;
import java.util.StringJoiner;
public final class VectorValues {
    public static final int DIMENSIONS = 768;
    private VectorValues() {}
    public static String literal(float[] vector) {
        if (vector == null || vector.length != DIMENSIONS)
            throw new IllegalArgumentException("Embedding must contain exactly 768 numbers");
        double norm = 0;
        var values = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Embedding numbers must be finite");
            norm += (double) value * value;
            values.add(Float.toString(value));
        }
        if (norm == 0) throw new IllegalArgumentException("Zero embedding cannot be used for cosine search");
        return values.toString();
    }
}
