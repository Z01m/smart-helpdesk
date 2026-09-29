package com.smarthelpdesk.aiworker.rag;

import com.smarthelpdesk.aiworker.ai.AiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiClient aiClient;

    public float[] embed(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "text cannot be null or blank"
            );
        }

        float[] result = aiClient.embed(text);

        if (result == null || result.length == 0) {
            throw new IllegalStateException(
                    "embedding result cannot be null or empty"
            );
        }

        return result;
    }

    public List<float[]> embedBatch(List<String> texts) {

        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException(
                    "texts cannot be null or empty"
            );
        }

        List<float[]> result = new ArrayList<>(texts.size());

        for (String text : texts) {
            result.add(embed(text));
        }

        return result;
    }
}
