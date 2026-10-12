package com.smarthelpdesk.knowledgebase.service;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChunkingService {
    // Character-based approximation of ~500 tokens, with overlap to preserve boundary context.
    private static final int CHUNK_SIZE = 2000;
    private static final int OVERLAP = 200;
    public List<String> split(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Article content must not be blank");
        var chunks = new ArrayList<String>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            // Do not split a UTF-16 surrogate pair (emoji, non-BMP characters).
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            String chunk = text.substring(start, end).strip();
            if (!chunk.isBlank()) chunks.add(chunk);
            if (end == text.length()) break;
            start = end - OVERLAP;
            if (Character.isLowSurrogate(text.charAt(start))) start--;
        }
        return List.copyOf(chunks);
    }
}
