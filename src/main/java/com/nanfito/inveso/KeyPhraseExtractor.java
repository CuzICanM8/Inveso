package com.nanfito.inveso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class KeyPhraseExtractor {
    public static final int CHUNK_SIZE = 1000;
    private final CoreNLPClient nlpClient;
    private static final Set<String> FILLER_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "where",
            "why", "how", "is", "are", "was", "were", "be", "been", "being", "of", "to",
            "in", "on", "with", "as", "by", "for", "about", "against", "between", "into",
            "through", "during", "before", "after", "above", "below", "from", "up", "down",
            "out", "off", "over", "under", "again", "further", "once", "here", "there",
            "all", "any", "both", "each", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s",
            "t", "can", "will", "just", "don", "should", "now"
    );

    public KeyPhraseExtractor() {
        this.nlpClient = new CoreNLPClient();
    }

    public List<String> extractKeyPhrases(String text) {
        try {
            List<String> keyPhrases = new ArrayList<>();
            // Clean the text to remove unrecognizable tokens
            String cleanedText = cleanText(text);
            List<String> chunks = chunkText(cleanedText, CHUNK_SIZE);
            for (String chunk : chunks) {
                String jsonResponse = nlpClient.analyzeText(chunk);
                keyPhrases.addAll(parseEntities(jsonResponse));
            }
            return filterKeyPhrases(keyPhrases);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private String cleanText(String text) {
        // Use a regex to keep only standard characters
        return text.replaceAll("[^\\w\\s,.!?;:@'\"()-/$]+", "").trim();
    }

    List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
        }
        return chunks;
    }

    private List<String> parseEntities(String jsonResponse) {
        List<String> phrases = new ArrayList<>();
        try {
            // Check if jsonResponse is a valid JSON
            if (jsonResponse.trim().startsWith("{")) {
                JSONObject root = new JSONObject(jsonResponse);
                JSONArray sentences = root.getJSONArray("sentences");

                for (int i = 0; i < sentences.length(); i++) {
                    JSONObject sentence = sentences.getJSONObject(i);
                    JSONArray entities = sentence.getJSONArray("entitymentions");

                    for (int j = 0; j < entities.length(); j++) {
                        JSONObject entity = entities.getJSONObject(j);
                        String entityText = entity.getString("text");
                        phrases.add(entityText);
                    }
                }
            } else {
                System.err.println("Invalid JSON response: " + jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return phrases;
    }

    private List<String> filterKeyPhrases(List<String> keyPhrases) {
        // Remove filler words and duplicates
        List<String> filteredPhrases = keyPhrases.stream()
                .map(String::toLowerCase)
                .filter(phrase -> !FILLER_WORDS.contains(phrase))
                .collect(Collectors.toList());

        // Count occurrences and filter out keywords that are scarcely repeated
        Map<String, Long> frequencyMap = filteredPhrases.stream()
                .collect(Collectors.groupingBy(phrase -> phrase, Collectors.counting()));

        return frequencyMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) // Adjust the threshold as needed
                .map(Map.Entry::getKey)
                .distinct()
                .collect(Collectors.toList());
    }
}
