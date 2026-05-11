package com.taskflow.automate.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility that auto-categorizes tasks by analyzing their title and description
 * against category keyword maps. Returns the best-matching category string.
 */
public class SmartTaskCategorizer {

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("Communication", new String[]{
                "reply", "respond", "call", "message", "email", "text", "ping",
                "reach out", "contact", "notify", "inform"
        });
        CATEGORY_KEYWORDS.put("Development", new String[]{
                "code", "deploy", "fix", "test", "pr", "merge", "branch",
                "build", "debug", "commit", "release", "refactor", "api"
        });
        CATEGORY_KEYWORDS.put("Documentation", new String[]{
                "document", "report", "write", "draft", "prepare",
                "presentation", "slides", "readme", "wiki", "spec"
        });
        CATEGORY_KEYWORDS.put("Meeting", new String[]{
                "meeting", "discuss", "sync", "standup", "review",
                "agenda", "call", "huddle", "brainstorm"
        });
        CATEGORY_KEYWORDS.put("Administrative", new String[]{
                "book", "order", "payment", "invoice", "register",
                "schedule", "form", "paperwork", "approve", "budget"
        });
        CATEGORY_KEYWORDS.put("Personal", new String[]{
                "pick up", "buy", "collect", "return", "grocery",
                "appointment", "gym", "doctor", "laundry"
        });
    }

    private static final String[] URGENCY_KEYWORDS = {
            "urgent", "asap", "critical", "blocker", "immediately"
    };

    /**
     * Analyzes combined title and description text against category keyword maps
     * and returns the best-matching category string.
     *
     * @param title       task title
     * @param description task description
     * @return the best-matching category, or "General" if no match found
     */
    public String categorize(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        String bestCategory = null;
        int bestScore = 0;

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (combined.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory != null ? bestCategory : "General";
    }

    /**
     * Suggests a priority based on urgency keywords and deadline proximity.
     *
     * @param title       task title
     * @param description task description
     * @param dueDate     due date in epoch millis, or null
     * @return priority: 1 (High), 2 (Medium), or 3 (Low)
     */
    public int suggestPriority(String title, String description, Long dueDate) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        // Check urgency keywords
        for (String keyword : URGENCY_KEYWORDS) {
            if (combined.contains(keyword)) {
                return 1;
            }
        }

        // Check due date proximity
        if (dueDate != null) {
            long now = System.currentTimeMillis();
            long diffMs = dueDate - now;

            if (diffMs <= 24 * 60 * 60 * 1000L) {
                // Due within 24 hours
                return 1;
            } else if (diffMs <= 3 * 24 * 60 * 60 * 1000L) {
                // Due within 3 days
                return 2;
            }
        }

        return 3;
    }
}
