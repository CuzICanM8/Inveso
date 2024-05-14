package com.nanfito.inveso;

import java.util.List;
import java.util.stream.Collectors;

public class TextFilter {
    /**
     * Filters and trims text to remove non-standard characters and limit length.
     * @param text the original text
     * @param maxLength the maximum allowed length of text
     * @return a filtered and possibly trimmed version of the text
     */
    public static String filterText(String text, int maxLength) {
        // Replace non-ASCII characters with a space or nothing
        String cleanedText = text.replaceAll("[^\\x20-\\x7E]", "");

        // Trim the text to the maximum length if necessary
        if (cleanedText.length() > maxLength) {
            cleanedText = cleanedText.substring(0, maxLength);
        }
        return cleanedText;
    }

    /**
     * Process a list of articles, filtering and trimming their descriptions.
     * @param articles the list of articles
     * @param maxLength the maximum length of each article description
     * @return a list of processed article descriptions
     */
    public static List<String> processArticles(List<Article> articles, int maxLength) {
        return articles.stream()
                .map(article -> filterText(article.getDescription(), maxLength))
                .collect(Collectors.toList());
    }
}
