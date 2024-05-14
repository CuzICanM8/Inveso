package com.nanfito.inveso;

import java.util.Date;
import java.util.Locale;

public class Article {
    private String title;
    private String url;
    private String source;

    private String description;
    private Date publicationDate;
    private double sentimentScore;
    private boolean isRelevant;

    public Article(String title, String url, String source, Date publicationDate, double sentimentScore, boolean isRelevant) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.publicationDate = publicationDate;
        this.sentimentScore = sentimentScore;
        this.isRelevant = isRelevant;
    }

    public Article(String title, String urlToArticle, String sourceName, String description) {
        this.title = title;
        this.url = urlToArticle;
        this.source = sourceName;
        this.description = description;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public double getSentimentScore() {
        return sentimentScore;
    }

    public boolean isRelevant() {
        return isRelevant;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setSentimentScore(double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public void setRelevant(boolean isRelevant) {
        this.isRelevant = isRelevant;
    }

    // Function to determine if the article should be included in the summary
    public boolean shouldIncludeInSummary() {
        // Exclude articles if the title isn't in English or is empty
        Locale locale = Locale.forLanguageTag("en");
        boolean isEnglish = title != null && title.toLowerCase(locale).matches("[a-z\\s\\p{Punct}]*");
        return isRelevant && sentimentScore >= 0 && isEnglish;
    }
}