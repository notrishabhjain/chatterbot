package com.taskflow.automate.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskExtractor {

    // Only block apps that NEVER produce useful notifications
    private static final Set<String> NON_ACTIONABLE_PACKAGES = new HashSet<>(Arrays.asList(
            "com.spotify.music",
            "com.google.android.music",
            "com.google.android.apps.youtube.music",
            "com.android.providers.downloads",
            "com.android.systemui",
            "android"
    ));

    // Apps whose notifications are always considered actionable
    private static final Set<String> ALWAYS_ACTIONABLE_PACKAGES = new HashSet<>(Arrays.asList(
            // Email
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.samsung.android.email.provider",
            "com.yahoo.mobile.client.android.mail",
            // Calendar
            "com.google.android.calendar",
            "com.samsung.android.calendar",
            // Messaging
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.slack",
            "com.Slack",
            "com.microsoft.teams",
            "org.thoughtcrime.securesms",
            "com.discord",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            // Productivity
            "com.google.android.apps.tasks",
            "com.todoist",
            "com.ticktick.task",
            "com.microsoft.todos",
            "com.asana.app",
            "com.trello",
            "notion.id",
            "com.atlassian.android.jira.core"
    ));

    private static final Set<String> ACTION_KEYWORDS = new HashSet<>(Arrays.asList(
            // English
            "please", "need", "review", "approve", "submit", "send",
            "complete", "follow up", "follow-up", "action required",
            "respond", "reply", "confirm", "schedule", "attend",
            "reminder", "deadline", "due", "meeting", "call",
            "urgent", "asap", "important", "todo", "task",
            "check", "update", "share", "prepare", "fix",
            "look into", "get back", "let me know", "can you",
            "could you", "would you", "will you", "make sure",
            // New English action words
            "assign", "deliver", "coordinate", "arrange", "organize",
            "handle", "resolve", "investigate", "implement", "finalize",
            "prioritize", "escalate", "document", "test", "deploy",
            "migrate", "integrate", "setup", "configure", "install",
            "download", "upload", "backup", "restore", "monitor",
            "track", "verify", "validate", "budget", "invoice",
            "payment", "transfer", "book", "reserve", "order",
            "purchase", "return", "exchange", "pickup", "drop",
            "collect", "submit", "register", "apply", "renew",
            "cancel", "reschedule",
            // Hindi (transliterated)
            "karo", "karna", "bhejo", "dekho", "batao", "kar do",
            "kar dena", "bhej do", "bhej dena", "check karo",
            "reply karo", "send karo", "complete karo", "jaldi",
            "zaruri", "zaroori", "jaruri", "important hai",
            "kal tak", "aaj", "abhi", "turant",
            // New Hindi/Hinglish transliterated words
            "bhejna", "dekhna", "banana", "likhna", "padhna",
            "samjhana", "sikhana", "baat karo", "discuss karo",
            "plan karo", "decide karo", "book karo", "order karo",
            "payment karo", "transfer karo", "register karo",
            "apply karo", "cancel karo", "update karo", "install karo",
            "download karo", "setup karo", "fix karo", "resolve karo",
            "test karo", "deploy karo", "jama karo", "collect karo",
            "submit karo", "prepare karo", "arrange karo",
            // Hindi (Unicode)
            "\u0915\u0930\u094B", "\u0915\u0930\u0928\u093E", "\u092D\u0947\u091C\u094B",
            "\u0926\u0947\u0916\u094B", "\u092C\u0924\u093E\u0913",
            "\u091C\u0930\u0942\u0930\u0940", "\u091C\u0932\u094D\u0926\u0940",
            "\u092E\u0940\u091F\u093F\u0902\u0917", "\u0915\u0949\u0932",
            "\u092F\u093E\u0926", "\u0930\u093F\u092E\u093E\u0907\u0902\u0921\u0930",
            // New Hindi Unicode keywords
            "\u092D\u0947\u091C\u0928\u093E", "\u0926\u0947\u0916\u0928\u093E",
            "\u092C\u0928\u093E\u0928\u093E", "\u0932\u093F\u0916\u0928\u093E",
            "\u092A\u0922\u093C\u0928\u093E", "\u0938\u092E\u091D\u093E\u0928\u093E",
            "\u0938\u093F\u0916\u093E\u0928\u093E", "\u092C\u0941\u0915",
            "\u0911\u0930\u094D\u0921\u0930", "\u092A\u0947\u092E\u0947\u0902\u091F",
            "\u091F\u094D\u0930\u093E\u0902\u0938\u092B\u0930", "\u0905\u092A\u0921\u0947\u091F",
            "\u0907\u0902\u0938\u094D\u091F\u0949\u0932", "\u0921\u093E\u0909\u0928\u0932\u094B\u0921",
            "\u0924\u0948\u092F\u093E\u0930", "\u091C\u092E\u093E", "\u0938\u092C\u092E\u093F\u091F"
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

        // Filter out non-actionable apps (system/media only)
        if (packageName != null && NON_ACTIONABLE_PACKAGES.contains(packageName)) {
            result.isActionable = false;
            return result;
        }

        // Filter out non-actionable notification patterns (media, downloads, system)
        if (isNonActionableContent(title, text)) {
            result.isActionable = false;
            return result;
        }

        String combinedText = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();

        // Skip very short/empty notifications (likely just app name or single emoji)
        if (combinedText.trim().length() < 3) {
            result.isActionable = false;
            return result;
        }

        // Always actionable packages - accept without keyword check
        if (packageName != null && ALWAYS_ACTIONABLE_PACKAGES.contains(packageName)) {
            result.isActionable = true;
            result.taskTitle = title != null ? title : "Notification Task";
            result.taskDescription = text != null ? text : "";
            result.dueDateHint = extractDueDateHint(combinedText);
            return result;
        }

        // For other apps: accept if it has meaningful text content (> 10 chars)
        // OR if it contains action keywords
        boolean hasMeaningfulContent = combinedText.trim().length() > 10;
        boolean hasActionKeyword = containsActionKeyword(combinedText);

        if (hasMeaningfulContent || hasActionKeyword) {
            result.isActionable = true;
            result.taskTitle = title != null ? title : "Notification Task";
            result.taskDescription = text != null ? text : "";
            result.dueDateHint = extractDueDateHint(combinedText);
            return result;
        }

        result.isActionable = false;
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

        // Generic group info notifications (WhatsApp group summaries like "5 new messages")
        if (combined.matches(".*\\d+\\s*(new\\s*)?messages?.*") && !combined.contains(":")) {
            return true;
        }

        return false;
    }

    private boolean containsActionKeyword(String combinedText) {
        for (String keyword : ACTION_KEYWORDS) {
            if (combinedText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Long extractDueDateHint(String text) {
        long now = System.currentTimeMillis();

        // Check "tomorrow" / "kal"
        if (text.contains("tomorrow") || text.contains("kal") || text.contains("\u0915\u0932")) {
            return now + 24 * 60 * 60 * 1000L;
        }

        // Check "today" or "EOD" or "end of day" or "aaj"
        if (text.contains("today") || text.contains("eod") || text.contains("end of day") ||
                text.contains("aaj") || text.contains("\u0906\u091C")) {
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
