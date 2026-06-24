package com.taskflow.automate.util;

import com.taskflow.automate.model.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects when a new task is similar to an existing pending task
 * by comparing normalized titles using word overlap.
 */
public class DuplicateTaskDetector {

    private static final float DUPLICATE_THRESHOLD = 0.70f;

    /**
     * Finds an existing task that is a near-duplicate of the new task.
     *
     * @param newTitle       the title of the new task
     * @param newDescription the description of the new task
     * @param existingTasks  list of existing pending tasks to check against
     * @return the matching existing Task if overlap > 70%, or null if no duplicate found
     */
    public Task findDuplicate(String newTitle, String newDescription, List<Task> existingTasks) {
        if (newTitle == null || newTitle.isEmpty() || existingTasks == null || existingTasks.isEmpty()) {
            return null;
        }

        for (Task existing : existingTasks) {
            if (existing.getTitle() == null || existing.getTitle().isEmpty()) {
                continue;
            }
            float similarity = calculateSimilarity(newTitle, existing.getTitle());
            if (similarity > DUPLICATE_THRESHOLD) {
                return existing;
            }
        }

        return null;
    }

    /**
     * Calculates the word overlap similarity between two texts.
     * Normalizes both texts (lowercase, strip punctuation, split into words)
     * and computes the ratio of overlapping words to the smaller word set size.
     *
     * @param text1 first text
     * @param text2 second text
     * @return similarity score from 0.0 to 1.0
     */
    public float calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0f;
        }

        Set<String> words1 = normalizeToWords(text1);
        Set<String> words2 = normalizeToWords(text2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0f;
        }

        // Calculate overlap
        int overlapCount = 0;
        for (String word : words1) {
            if (words2.contains(word)) {
                overlapCount++;
            }
        }

        // Use the smaller set size as the denominator for ratio
        int minSize = Math.min(words1.size(), words2.size());
        return (float) overlapCount / minSize;
    }

    private Set<String> normalizeToWords(String text) {
        Set<String> words = new HashSet<>();
        // Lowercase and strip punctuation
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part);
            }
        }
        return words;
    }
}
