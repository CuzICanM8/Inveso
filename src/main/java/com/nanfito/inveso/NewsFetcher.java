package com.nanfito.inveso;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsFetcher {
    private static final String API_KEY = "ee4a241ae5414320928107a66dfea0a9";  // Replace with your actual API key
    private static final String BASE_URL = "https://newsapi.org/v2/everything";

    public List<Article> fetchNewsArticles(String query) {
        List<Article> articles = new ArrayList<>();

        try {
            String urlString = BASE_URL + "?q=" + query + "&apiKey=" + API_KEY + "&language=en";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Failed: HTTP error code: " + connection.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            connection.disconnect();

            JSONObject jsonObject = new JSONObject(response.toString());
            JSONArray articlesArray = jsonObject.getJSONArray("articles");

            for (int i = 0; i < articlesArray.length(); i++) {
                JSONObject articleJson = articlesArray.getJSONObject(i);
                String sourceName = articleJson.getJSONObject("source").getString("name");
                String title = articleJson.getString("title");
                String description = articleJson.optString("description", "No description available.");
                String urlToArticle = articleJson.getString("url");

                articles.add(new Article(title, urlToArticle, sourceName, description));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return articles;
    }
}
