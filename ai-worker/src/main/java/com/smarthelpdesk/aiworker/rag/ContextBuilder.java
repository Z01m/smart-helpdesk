package com.smarthelpdesk.aiworker.rag;

import com.smarthelpdesk.aiworker.dto.knowledge.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ContextBuilder {

    private static final int APPROX_CHARS_PER_TOKEN = 4;

    public String build(List<ScoredChunk> chunks, int maxTokens) {

        if (chunks == null) {
            throw new IllegalArgumentException("chunks must not be null");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be greater than 0");
        }
        if (chunks.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        context.append("[КОНТЕКСТ ИЗ БАЗЫ ЗНАНИЙ]\n\n");

        Set<String> uniqueTexts = new HashSet<>();

        int currentTokens = estimateTokens(context.toString());
        int index = 1;

        for (ScoredChunk chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            String text = chunk.text();

            if (text == null || text.isBlank()) {
                continue;
            }

            text = text.trim();

            if (!uniqueTexts.add(text)) {
                continue;
            }

            String title = chunk.title();

            if (title == null || title.isBlank()) {
                title = "Без названия";
            }

            String header = index + ". " + title.trim() + "\nРелевантность: " + String.format(Locale.ROOT, "%.2f", chunk.score()) + "\n";

            int headerTokens = estimateTokens(header);

            int remainingTokens = maxTokens - currentTokens - headerTokens;

            if (remainingTokens <= 0) {
                break;
            }

            String preparedText = truncateToTokenLimit(text, remainingTokens);

            if (preparedText.isBlank()) {
                break;
            }

            String block = header + preparedText + "\n\n";

            int blockTokens = estimateTokens(block);

            if (currentTokens + blockTokens > maxTokens) {
                break;
            }
            context.append(block);

            currentTokens += blockTokens;
            index++;
        }

        if (index == 1) {
            return "";
        }

        return context.toString().trim();
    }

    private int estimateTokens(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        return (int) Math.ceil((double) text.length() / APPROX_CHARS_PER_TOKEN
        );
    }
    private String truncateToTokenLimit(String text, int maxTokens) {

        int estimatedTokens = estimateTokens(text);

        if (estimatedTokens <= maxTokens) {
            return text;
        }

        int maxCharacters = maxTokens * APPROX_CHARS_PER_TOKEN;

        if (maxCharacters <= 3) {
            return "";
        }

        return text
                .substring(0, Math.min(text.length(), maxCharacters - 3))
                .trim()
                + "...";
    }
}