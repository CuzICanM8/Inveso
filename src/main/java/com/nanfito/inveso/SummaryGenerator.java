package com.nanfito.inveso;

import java.util.List;
import java.util.StringJoiner;

public class SummaryGenerator {

    /**
     * Generate a summary based on sentiment scores and key phrases.
     */
    public String generateSummary(List<Article> articles, List<Double> sentimentScores, List<String> keyPhrases) {
        StringBuilder summary = new StringBuilder("Summary of Articles:\n\n");

        // Check if the lists have different sizes
        if (articles.size() != sentimentScores.size()) {
            System.err.println("Mismatch in sizes: articles.size() = " + articles.size() + ", sentimentScores.size() = " + sentimentScores.size());
            return "Error: Mismatch in articles and sentiment scores.";
        }

        double averageSentiment = sentimentScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        summary.append(String.format("Average Sentiment Score: %.2f\n", averageSentiment));

        summary.append("Key Phrases: ");
        StringJoiner joiner = new StringJoiner(", ");
        keyPhrases.forEach(joiner::add);
        summary.append(joiner.toString()).append("\n\n");

        summary.append("Detailed Insights:\n");

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            double score = sentimentScores.get(i);
            summary.append(String.format("- %s: %s (Score: %.2f)\n", article.getTitle(), convertScoreToLabel(score), score));
        }

        return summary.toString();
    }

    /**
     * Convert sentiment scores to human-readable labels.
     */
    private String convertScoreToLabel(double score) {
        if (score >= 0.5) return "Positive";
        else if (score > -0.5) return "Neutral";
        else return "Negative";
    }
}
