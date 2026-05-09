package com.taskflow.automate.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskExtractor {

    private static final Set<String> NON_ACTIONABLE_PACKAGES = new HashSet<>(Arrays.asList(
            "com.spotify.music",
            "com.google.android.music",
            "com.google.android.apps.youtube.music",
            "com.android.providers.downloads",
            "com.android.vending",
            "com.android.systemui",
            "android"
    ));

    private static final Set<String> ACTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "please", "need", "review", "approve", "submit", "send",
            "complete", "follow up", "follow-up", "action required",
            "respond", "reply", "confirm", "schedule", "attend",
            "reminder", "deadline", "due", "meeting", "call"
    ));

    private static final Pattern TIME_PATTERN_AT = Pattern.compile(
            "(?i)\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b"
    );

    private static final Pattern TIME_PATTERN_BY = Pattern.compile(
            "(?i)\\bby\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b"
    );

    private static final Pattern RELATIVE_HOURS = Pattern.compile(
            "(?i)\\bin\\s+(\\d+)\\s*hours?\\b"
    );

    private static final Pattern RELATIVE_MINUTES = Pattern.compile(
            "(?i)\\bin\\s+(\\d+)\\s*minutes?\\b"
    );

    public TaskExtractionResult extractTask(String title, String text, String packageName) {
        TaskExtractionResult result = new TaskExtractionResult();

        // Filter out non-actionable apps
        if (packageName != null && NON_ACTIONABLE_PACKAGES.contains(packageName)) {
            result.isActionable = false;
            return result;
        }

        // Filter out non-actionable notification patterns
        if (isNonActionableContent(title, text)) {
            result.isActionable = false;
            return result;
        }

        // Check if notification content contains actionable patterns
        String combinedText = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();

        if (!containsActionablePattern(combinedText, packageName)) {
            result.isActionable = false;
            return result;
        }

        // Extract task information
        result.isActionable = true;
        result.taskTitle = title != null ? title : "Notification Task";
        result.taskDescription = text != null ? text : "";
        result.dueDateHint = extractDueDateHint(combinedText);

        return result;
    }

    private boolean isNonActionableContent(String title, String text) {
        String combined = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();

        // Music/media playback
        if (combined.contains("now playing") || combined.contains("playing:") ||
                combined.contains("paused") || combined.contains("track")) {
            return true;
        }

        // Downloads
        if (combined.contains("downloading") || combined.contains("download complete") ||
                combined.contains("upload complete")) {
            return true;
        }

        // USB/charging/system
        if (combined.contains("usb") || combined.contains("charging") ||
                combined.contains("battery") || combined.contains("connected to")) {
            return true;
        }

        return false;
    }

    private boolean containsActionablePattern(String combinedText, String packageName) {
        // Calendar and email apps are inherently more actionable
        if (packageName != null) {
            if (packageName.contains("calendar") || packageName.contains("gmail") ||
                    packageName.contains("email") || packageName.contains("outlook")) {
                return true;
            }
        }

        // Check for action keywords
        for (String keyword : ACTION_KEYWORDS) {
            if (combinedText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private Long extractDueDateHint(String text) {
        long now = System.currentTimeMillis();

        // Check "tomorrow"
        if (text.contains("tomorrow")) {
            return now + 24 * 60 * 60 * 1000L;
        }

        // Check "today" or "EOD" or "end of day"
        if (text.contains("today") || text.contains("eod") || text.contains("end of day")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        // Check "in X hours"
        Matcher hoursMatcher = RELATIVE_HOURS.matcher(text);
        if (hoursMatcher.find()) {
            int hours = Integer.parseInt(hoursMatcher.group(1));
            return now + hours * 60 * 60 * 1000L;
        }

        // Check "in X minutes"
        Matcher minutesMatcher = RELATIVE_MINUTES.matcher(text);
        if (minutesMatcher.find()) {
            int minutes = Integer.parseInt(minutesMatcher.group(1));
            return now + minutes * 60 * 1000L;
        }

        // Check "at X:XX pm/am" or "by X:XX pm/am"
        Long atTime = extractTimeFromPattern(text, TIME_PATTERN_AT);
        if (atTime != null) {
            return atTime;
        }

        Long byTime = extractTimeFromPattern(text, TIME_PATTERN_BY);
        if (byTime != null) {
            return byTime;
        }

        return null;
    }

    private Long extractTimeFromPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            String amPm = matcher.group(3);

            if (amPm != null) {
                if (amPm.equalsIgnoreCase("pm") && hour != 12) {
                    hour += 12;
                } else if (amPm.equalsIgnoreCase("am") && hour == 12) {
                    hour = 0;
                }
            }

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // If the time has already passed today, assume tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            return cal.getTimeInMillis();
        }
        return null;
    }

    public static class TaskExtractionResult {
        public boolean isActionable;
        public String taskTitle;
        public String taskDescription;
        public Long dueDateHint;

        public TaskExtractionResult() {
            this.isActionable = false;
            this.taskTitle = null;
            this.taskDescription = null;
            this.dueDateHint = null;
        }
    }
}
