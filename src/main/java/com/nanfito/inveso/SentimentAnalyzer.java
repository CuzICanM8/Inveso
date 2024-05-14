package com.nanfito.inveso;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SentimentAnalyzer {
    // CoreNLP server URL for analyzing sentiments
    private static final String CORENLP_SERVER_URL = "http://10.0.0.133:9000";

    private ExecutorService executor = Executors.newSingleThreadExecutor();  // Single-threaded executor to queue tasks

    public CompletableFuture<Double> analyzeSentiments(List<String> articles) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                double totalScore = 0;
                int validScores = 0;

                for (String text : articles) {
                    String properties = URLEncoder.encode("{'annotators':'tokenize,ssplit,pos,parse,sentiment','outputFormat':'json'}", StandardCharsets.UTF_8);
                    String sentimentJson = Request.Post(CORENLP_SERVER_URL + "/?properties=" + properties)
                            .bodyString(text, ContentType.TEXT_PLAIN)
                            .execute()
                            .returnContent()
                            .asString();

                    JSONObject root = new JSONObject(sentimentJson);
                    JSONArray sentences = root.getJSONArray("sentences");
                    for (int i = 0; i < sentences.length(); i++) {
                        JSONObject sentence = sentences.getJSONObject(i);
                        String sentiment = sentence.getString("sentiment");
                        double score = sentimentToScore(sentiment);
                        if (score != 0) {  // Exclude neutral scores
                            totalScore += score;
                            validScores++;
                        }
                    }
                }

                double averageScore = validScores > 0 ? totalScore / validScores : 0;
                future.complete(averageScore);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private double parseSentimentScore(String sentimentJson) {
        try {
            // Parse JSON response to extract sentiment scores
            JSONObject root = new JSONObject(sentimentJson);
            JSONArray sentences = root.getJSONArray("sentences");
            double totalScore = 0;
            int count = 0;

            for (int i = 0; i < sentences.length(); i++) {
                JSONObject sentence = sentences.getJSONObject(i);
                String sentiment = sentence.getString("sentiment");
                double locScore = sentimentToScore(sentiment);
                if (locScore != -1.0 && locScore != 0.0) {
                    totalScore += locScore;
                    count++;
                }
            }

            return count > 0 ? totalScore / count : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Convert sentiment labels to numerical scores
    private double sentimentToScore(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "very positive":
                return 2.0;
            case "positive":
                return 1.0;
            case "neutral":
                return 0.0;
            case "negative":
                return -1.0;
            case "very negative":
                return -2.0;
            default:
                return 0;
        }
    }
}
