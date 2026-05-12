package com.taskflow.automate.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts meaningful keywords from manually-added tasks to expand
 * the notification task detection dictionary over time.
 */
public class KeywordLearner {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "shall", "can", "need", "dare", "ought",
        "used", "to", "of", "in", "for", "on", "with", "at", "by", "from",
        "as", "into", "through", "during", "before", "after", "above", "below",
        "between", "out", "off", "over", "under", "again", "further", "then",
        "once", "here", "there", "when", "where", "why", "how", "all", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "just",
        "because", "but", "and", "or", "if", "while", "about", "up", "down",
        "that", "this", "these", "those", "am", "it", "its", "my", "me",
        "we", "our", "you", "your", "he", "she", "they", "them", "his", "her",
        "i", "what", "which", "who", "whom", "whose", "get", "got", "make"
    ));

    /**
     * Extracts meaningful keywords from a task title and description.
     * Filters out stop words, short words, and returns lowercase keywords.
     */
    public static Set<String> extractKeywords(String title, String description) {
        Set<String> keywords = new HashSet<>();

        String combined = "";
        if (title != null) combined += title;
        if (description != null) combined += " " + description;

        // Tokenize: split on whitespace and punctuation
        String[] tokens = combined.toLowerCase().split("[\\s,;:.!?()\\[\\]{}\"']+");

        for (String token : tokens) {
            token = token.trim();
            // Skip short words, stop words, and pure numbers
            if (token.length() < 3) continue;
            if (STOP_WORDS.contains(token)) continue;
            if (token.matches("\\d+")) continue;
            keywords.add(token);
        }

        return keywords;
    }

    /**
     * Determines if keywords should be learned from this task.
     * Only learn from manually-created tasks.
     */
    public static boolean shouldLearnFromTask(String sourceApp) {
        return "Manual".equals(sourceApp);
    }
}
