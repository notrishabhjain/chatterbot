package com.taskflow.automate.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskExtractor {

    private static final int ACTIONABILITY_THRESHOLD = 25;

    private static final Set<String> NON_ACTIONABLE_PACKAGES = new HashSet<>(Arrays.asList(
            "com.spotify.music",
            "com.google.android.music",
            "com.google.android.apps.youtube.music",
            "com.android.providers.downloads",
            "com.android.vending",
            "com.android.systemui",
            "android"
    ));

    private static final Set<String> HIGH_REPUTATION_PACKAGES = new HashSet<>(Arrays.asList(
            "com.google.android.calendar",
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.microsoft.office.officehubrow",
            "com.samsung.android.email.provider"
    ));

    private static final Set<String> MEDIUM_REPUTATION_PACKAGES = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.slack",
            "org.telegram.messenger",
            "com.Slack",
            "com.microsoft.teams"
    ));

    private static final Set<String> MESSAGING_PACKAGES = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.slack",
            "com.Slack",
            "com.microsoft.teams",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
    ));

    // High-weight action keywords (15 points each)
    private static final Set<String> HIGH_ACTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "urgent", "asap", "immediately", "action required", "critical",
            "time-sensitive"
    ));

    // Medium-weight action keywords (10 points each)
    private static final Set<String> MEDIUM_ACTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "please", "need", "review", "approve", "submit", "send",
            "complete", "follow up", "follow-up", "respond", "reply",
            "confirm", "schedule", "attend", "reminder", "deadline",
            "due", "meeting", "call", "can you", "could you", "would you",
            "need you to", "help with"
    ));

    // Task type keyword sets
    private static final Set<String> MEETING_KEYWORDS = new HashSet<>(Arrays.asList(
            "meeting", "standup", "sync", "call", "conference", "zoom", "teams"
    ));

    private static final Set<String> DEADLINE_KEYWORDS = new HashSet<>(Arrays.asList(
            "deadline", "due", "submit", "turn in", "eod", "end of day"
    ));

    private static final Set<String> FOLLOW_UP_KEYWORDS = new HashSet<>(Arrays.asList(
            "follow up", "following up", "waiting for", "pending response",
            "any update", "checking in"
    ));

    private static final Set<String> REQUEST_KEYWORDS = new HashSet<>(Arrays.asList(
            "please", "can you", "could you", "would you", "need you to", "help with"
    ));

    private static final Set<String> APPROVAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "approve", "review", "sign off", "green light", "permission"
    ));

    private static final Set<String> REMINDER_KEYWORDS = new HashSet<>(Arrays.asList(
            "remind", "don't forget", "remember", "heads up"
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

    private static final Pattern NEXT_DAY_PATTERN = Pattern.compile(
            "(?i)\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"
    );

    private static final Pattern THIS_DAY_PATTERN = Pattern.compile(
            "(?i)\\bthis\\s+(friday|monday|tuesday|wednesday|thursday|saturday|sunday)\\b"
    );

    private static final Pattern QUESTION_PATTERN = Pattern.compile(
            "(?i)(can you|could you|would you|will you|are you able to|mind|please)"
    );

    /**
     * Extract task from notification content using weighted scoring system.
     *
     * @param title       notification title
     * @param text        notification text
     * @param bigText     notification big text (expanded content)
     * @param subText     notification sub text
     * @param packageName source app package name
     * @return TaskExtractionResult with scoring details
     */
    public TaskExtractionResult extractTask(String title, String text, String bigText,
                                            String subText, String packageName) {
        TaskExtractionResult result = new TaskExtractionResult();

        // Filter out non-actionable apps early
        if (packageName != null && NON_ACTIONABLE_PACKAGES.contains(packageName)) {
            result.isActionable = false;
            result.actionabilityScore = 0;
            return result;
        }

        // Combine all text fields for analysis
        String combinedText = combineText(title, text, bigText, subText);
        String lowerCombined = combinedText.toLowerCase();

        // Store source notification text
        result.sourceNotificationText = combinedText;

        // Filter out non-actionable content patterns
        if (isNonActionableContent(lowerCombined)) {
            result.isActionable = false;
            result.actionabilityScore = 0;
            return result;
        }

        // Calculate actionability score using weighted factors
        int score = 0;

        // Factor 1: Action keywords (high weight)
        score += getHighActionKeywordScore(lowerCombined);

        // Factor 2: Action keywords (medium weight)
        score += getMediumActionKeywordScore(lowerCombined);

        // Factor 3: Source app reputation
        score += getAppReputationScore(packageName);

        // Factor 4: Time reference presence
        score += getTimeReferenceScore(lowerCombined);

        // Factor 5: Message length heuristic
        score += getMessageLengthScore(lowerCombined);

        // Factor 6: Question/request detection
        score += getQuestionScore(lowerCombined);

        result.actionabilityScore = Math.min(score, 100);
        result.isActionable = result.actionabilityScore >= ACTIONABILITY_THRESHOLD;

        if (result.isActionable) {
            result.taskTitle = title != null && !title.isEmpty() ? title : "Notification Task";
            result.taskDescription = text != null ? text : (bigText != null ? bigText : "");
            result.dueDateHint = extractDueDateHint(lowerCombined);
            result.assigner = extractAssigner(title, packageName);
            result.taskType = classifyTaskType(lowerCombined);
            result.isFollowUp = detectFollowUp(lowerCombined);
        }

        return result;
    }

    /**
     * Legacy method for backward compatibility. Delegates to the new full method.
     */
    public TaskExtractionResult extractTask(String title, String text, String packageName) {
        return extractTask(title, text, null, null, packageName);
    }

    private String combineText(String title, String text, String bigText, String subText) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title);
        }
        if (text != null && !text.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(text);
        }
        if (bigText != null && !bigText.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(bigText);
        }
        if (subText != null && !subText.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(subText);
        }
        return sb.toString();
    }

    private boolean isNonActionableContent(String lowerCombined) {
        // Music/media playback
        if (lowerCombined.contains("now playing") || lowerCombined.contains("playing:") ||
                lowerCombined.contains("paused") || lowerCombined.contains("track")) {
            return true;
        }

        // Downloads
        if (lowerCombined.contains("downloading") || lowerCombined.contains("download complete") ||
                lowerCombined.contains("upload complete")) {
            return true;
        }

        // USB/charging/system
        if (lowerCombined.contains("usb") || lowerCombined.contains("charging") ||
                lowerCombined.contains("battery") || lowerCombined.contains("connected to")) {
            return true;
        }

        return false;
    }

    private int getHighActionKeywordScore(String lowerCombined) {
        int score = 0;
        for (String keyword : HIGH_ACTION_KEYWORDS) {
            if (lowerCombined.contains(keyword)) {
                score += 15;
            }
        }
        return Math.min(score, 30); // Cap at 30
    }

    private int getMediumActionKeywordScore(String lowerCombined) {
        int score = 0;
        for (String keyword : MEDIUM_ACTION_KEYWORDS) {
            if (lowerCombined.contains(keyword)) {
                score += 10;
            }
        }
        return Math.min(score, 30); // Cap at 30
    }

    private int getAppReputationScore(String packageName) {
        if (packageName == null) {
            return 0;
        }
        if (HIGH_REPUTATION_PACKAGES.contains(packageName)) {
            return 20;
        }
        if (MEDIUM_REPUTATION_PACKAGES.contains(packageName)) {
            return 10;
        }
        // Check by package name substring for calendar/email apps
        if (packageName.contains("calendar") || packageName.contains("gmail") ||
                packageName.contains("email") || packageName.contains("outlook")) {
            return 20;
        }
        return 0;
    }

    private int getTimeReferenceScore(String lowerCombined) {
        if (lowerCombined.contains("tomorrow") || lowerCombined.contains("today") ||
                lowerCombined.contains("eod") || lowerCombined.contains("end of day") ||
                lowerCombined.contains("end of week") || lowerCombined.contains("this friday") ||
                lowerCombined.contains("next monday") || lowerCombined.contains("next tuesday") ||
                lowerCombined.contains("next wednesday") || lowerCombined.contains("next thursday") ||
                lowerCombined.contains("next friday")) {
            return 15;
        }
        Matcher hoursMatcher = RELATIVE_HOURS.matcher(lowerCombined);
        if (hoursMatcher.find()) {
            return 15;
        }
        Matcher minutesMatcher = RELATIVE_MINUTES.matcher(lowerCombined);
        if (minutesMatcher.find()) {
            return 15;
        }
        Matcher atMatcher = TIME_PATTERN_AT.matcher(lowerCombined);
        if (atMatcher.find()) {
            return 10;
        }
        Matcher byMatcher = TIME_PATTERN_BY.matcher(lowerCombined);
        if (byMatcher.find()) {
            return 10;
        }
        return 0;
    }

    private int getMessageLengthScore(String lowerCombined) {
        int length = lowerCombined.trim().length();
        // Very short messages are less likely tasks
        if (length <= 5) {
            return -10;
        } else if (length <= 15) {
            return -5;
        } else if (length >= 30) {
            return 5;
        }
        return 0;
    }

    private int getQuestionScore(String lowerCombined) {
        Matcher matcher = QUESTION_PATTERN.matcher(lowerCombined);
        if (matcher.find()) {
            return 10;
        }
        return 0;
    }

    private String extractAssigner(String title, String packageName) {
        if (title == null || title.isEmpty()) {
            return null;
        }
        // Only extract assigner from messaging app notifications where the title
        // is typically the sender's name
        if (packageName == null || !MESSAGING_PACKAGES.contains(packageName)) {
            return null;
        }
        // Skip generic titles that are not person names
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.equals("notification") || lowerTitle.equals("new message") ||
                lowerTitle.equals("alert") || lowerTitle.equals("reminder") ||
                lowerTitle.equals("system") || lowerTitle.equals("update")) {
            return null;
        }
        return title;
    }

    private String classifyTaskType(String lowerCombined) {
        // Score each category by counting keyword hits
        int meetingScore = countKeywordHits(lowerCombined, MEETING_KEYWORDS);
        int deadlineScore = countKeywordHits(lowerCombined, DEADLINE_KEYWORDS);
        int followUpScore = countKeywordHits(lowerCombined, FOLLOW_UP_KEYWORDS);
        int approvalScore = countKeywordHits(lowerCombined, APPROVAL_KEYWORDS);
        int requestScore = countKeywordHits(lowerCombined, REQUEST_KEYWORDS);
        int reminderScore = countKeywordHits(lowerCombined, REMINDER_KEYWORDS);

        // Find the category with the most hits
        // Priority order for tiebreaker: MEETING > DEADLINE > FOLLOW_UP > APPROVAL > REQUEST > REMINDER
        String bestType = "GENERAL";
        int bestScore = 0;

        if (meetingScore > bestScore) {
            bestScore = meetingScore;
            bestType = "MEETING";
        }
        if (deadlineScore > bestScore) {
            bestScore = deadlineScore;
            bestType = "DEADLINE";
        }
        if (followUpScore > bestScore) {
            bestScore = followUpScore;
            bestType = "FOLLOW_UP";
        }
        if (approvalScore > bestScore) {
            bestScore = approvalScore;
            bestType = "APPROVAL";
        }
        if (requestScore > bestScore) {
            bestScore = requestScore;
            bestType = "REQUEST";
        }
        if (reminderScore > bestScore) {
            bestScore = reminderScore;
            bestType = "REMINDER";
        }

        return bestType;
    }

    private int countKeywordHits(String text, Set<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private boolean detectFollowUp(String lowerCombined) {
        for (String keyword : FOLLOW_UP_KEYWORDS) {
            if (lowerCombined.contains(keyword)) {
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

        // Check "end of week" or "this friday"
        if (text.contains("end of week") || text.contains("this friday")) {
            Calendar cal = Calendar.getInstance();
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int daysUntilFriday = Calendar.FRIDAY - dayOfWeek;
            if (daysUntilFriday <= 0) {
                daysUntilFriday += 7;
            }
            cal.add(Calendar.DAY_OF_MONTH, daysUntilFriday);
            cal.set(Calendar.HOUR_OF_DAY, 17);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        // Check "this <day>"
        Matcher thisDayMatcher = THIS_DAY_PATTERN.matcher(text);
        if (thisDayMatcher.find()) {
            String dayName = thisDayMatcher.group(1).toLowerCase();
            return getNextDayOfWeek(dayName);
        }

        // Check "next <day>"
        Matcher nextDayMatcher = NEXT_DAY_PATTERN.matcher(text);
        if (nextDayMatcher.find()) {
            String dayName = nextDayMatcher.group(1).toLowerCase();
            return getNextDayOfWeek(dayName);
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

    private Long getNextDayOfWeek(String dayName) {
        int targetDay;
        switch (dayName) {
            case "monday": targetDay = Calendar.MONDAY; break;
            case "tuesday": targetDay = Calendar.TUESDAY; break;
            case "wednesday": targetDay = Calendar.WEDNESDAY; break;
            case "thursday": targetDay = Calendar.THURSDAY; break;
            case "friday": targetDay = Calendar.FRIDAY; break;
            case "saturday": targetDay = Calendar.SATURDAY; break;
            case "sunday": targetDay = Calendar.SUNDAY; break;
            default: return null;
        }

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntil = targetDay - currentDay;
        if (daysUntil <= 0) {
            daysUntil += 7;
        }
        cal.add(Calendar.DAY_OF_MONTH, daysUntil);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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
        public String assigner;
        public String taskType;
        public boolean isFollowUp;
        public int actionabilityScore;
        public String sourceNotificationText;

        public TaskExtractionResult() {
            this.isActionable = false;
            this.taskTitle = null;
            this.taskDescription = null;
            this.dueDateHint = null;
            this.assigner = null;
            this.taskType = "GENERAL";
            this.isFollowUp = false;
            this.actionabilityScore = 0;
            this.sourceNotificationText = null;
        }
    }
}
