package com.taskflow.automate.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PriorityAssigner {

    private static final Set<String> HIGH_PRIORITY_APPS = new HashSet<>(Arrays.asList(
            "com.google.android.calendar",
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.microsoft.office.officehubrow",
            "com.samsung.android.email.provider"
    ));

    private static final Set<String> MEDIUM_PRIORITY_APPS = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.slack",
            "org.telegram.messenger",
            "com.Slack",
            "com.microsoft.teams"
    ));

    private static final Set<String> LOW_PRIORITY_APPS = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.snapchat.android"
    ));

    private static final Set<String> BOOST_KEYWORDS = new HashSet<>(Arrays.asList(
            "urgent", "asap", "deadline", "important", "due",
            "meeting", "critical", "immediately", "priority",
            "time-sensitive", "action required"
    ));

    /**
     * Assigns a priority level based on multiple factors.
     *
     * @param packageName source app package name
     * @param title       notification title
     * @param text        notification text
     * @param dueDate     extracted due date (nullable, epoch millis)
     * @return priority level: 1 (High), 2 (Medium), or 3 (Low)
     */
    public int assignPriority(String packageName, String title, String text, Long dueDate) {
        return assignPriority(packageName, title, text, dueDate, null);
    }

    /**
     * Assigns a priority level based on multiple factors including task type.
     *
     * @param packageName source app package name
     * @param title       notification title
     * @param text        notification text
     * @param dueDate     extracted due date (nullable, epoch millis)
     * @param taskType    classified task type (nullable)
     * @return priority level: 1 (High), 2 (Medium), or 3 (Low)
     */
    public int assignPriority(String packageName, String title, String text, Long dueDate, String taskType) {
        int score = 0;

        // Factor 1: Source app scoring
        score += getAppScore(packageName);

        // Factor 2: Keyword boosting
        score += getKeywordScore(title, text);

        // Factor 3: Time sensitivity
        score += getTimeSensitivityScore(dueDate);

        // Factor 4: Task type scoring
        score += getTaskTypeScore(taskType, dueDate);

        // Convert score to priority level
        if (score >= 3) {
            return 1; // High
        } else if (score >= 1) {
            return 2; // Medium
        } else {
            return 3; // Low
        }
    }

    private int getAppScore(String packageName) {
        if (packageName == null) {
            return 0;
        }

        if (HIGH_PRIORITY_APPS.contains(packageName)) {
            return 2;
        } else if (MEDIUM_PRIORITY_APPS.contains(packageName)) {
            return 1;
        } else if (LOW_PRIORITY_APPS.contains(packageName)) {
            return -1;
        }

        return 0;
    }

    private int getKeywordScore(String title, String text) {
        String combined = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();
        int boostCount = 0;

        for (String keyword : BOOST_KEYWORDS) {
            if (combined.contains(keyword)) {
                boostCount++;
            }
        }

        if (boostCount >= 3) {
            return 3;
        } else if (boostCount >= 2) {
            return 2;
        } else if (boostCount >= 1) {
            return 1;
        }

        return 0;
    }

    private int getTimeSensitivityScore(Long dueDate) {
        if (dueDate == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long diffMillis = dueDate - now;

        if (diffMillis <= 0) {
            // Already overdue
            return 3;
        } else if (diffMillis <= 60 * 60 * 1000L) {
            // Due within 1 hour
            return 3;
        } else if (diffMillis <= 24 * 60 * 60 * 1000L) {
            // Due within 24 hours
            return 1;
        }

        return 0;
    }

    private int getTaskTypeScore(String taskType, Long dueDate) {
        if (taskType == null) {
            return 0;
        }

        switch (taskType) {
            case "DEADLINE":
            case "APPROVAL":
                return 1;
            case "MEETING":
                // Meeting gets +1 if due within 2 hours
                if (dueDate != null) {
                    long now = System.currentTimeMillis();
                    long diffMillis = dueDate - now;
                    if (diffMillis > 0 && diffMillis <= 2 * 60 * 60 * 1000L) {
                        return 1;
                    }
                }
                return 0;
            default:
                return 0;
        }
    }
}
