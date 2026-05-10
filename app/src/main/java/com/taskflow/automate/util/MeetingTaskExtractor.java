package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeetingTaskExtractor {

    public enum Language { ENGLISH, HINDI, HINGLISH }

    public static class ExtractedActionItem {
        public String rawText;
        public String title;
        public Language detectedLanguage;
        public Long dueDate;       // nullable, epoch millis
        public String assigneeName; // nullable
        public float confidence;    // 0.0 to 1.0
    }

    private List<String> teamMemberNames = new ArrayList<>();

    // English action patterns with confidence levels
    private static final PatternEntry[] ENGLISH_PATTERNS = {
        new PatternEntry(Pattern.compile("(?i)\\b(I will|I'll|I am going to|I'm going to)\\b(.+)"), 0.9f),
        new PatternEntry(Pattern.compile("(?i)\\b(action item|TODO|task)\\b[:\\s]*(.+)?"), 0.9f),
        new PatternEntry(Pattern.compile("(?i)\\b(need to|have to|must)\\b(.+)"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(please|pls)\\b(.+)"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(follow up|follow-up)\\b(.+)?"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(make sure|ensure|take care of)\\b(.+)"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(let me|let's)\\b(.+)"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(deadline|due)\\b[:\\s]*(.+)?"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)\\b(should)\\b(.+)"), 0.5f),
        new PatternEntry(Pattern.compile("(?i)\\b(will|shall)\\b(.+)"), 0.5f)
    };

    // Hindi action patterns (Unicode)
    private static final PatternEntry[] HINDI_PATTERNS = {
        new PatternEntry(Pattern.compile("(.+)करना है"), 0.9f),
        new PatternEntry(Pattern.compile("(.+)करो"), 0.7f),
        new PatternEntry(Pattern.compile("(.+)भेजो"), 0.7f),
        new PatternEntry(Pattern.compile("(.+)करना होगा"), 0.9f),
        new PatternEntry(Pattern.compile("(.+)भेजना है"), 0.7f),
        new PatternEntry(Pattern.compile("मुझे(.+)"), 0.9f),
        new PatternEntry(Pattern.compile("(.+)ज़रूरी है"), 0.7f),
        new PatternEntry(Pattern.compile("(.+)ज़रूरी"), 0.5f),
        new PatternEntry(Pattern.compile("deadline है(.+)?"), 0.7f)
    };

    // Hinglish action patterns (mixed English/Hindi romanized)
    private static final PatternEntry[] HINGLISH_PATTERNS = {
        new PatternEntry(Pattern.compile("(?i)mujhe(.+)karna hai"), 0.9f),
        new PatternEntry(Pattern.compile("(?i)please(.+)kar do"), 0.9f),
        new PatternEntry(Pattern.compile("(?i)(.+)send karo"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)review kar lena"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)check karo"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)complete karo"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)karna padega"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)kar dena"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)bhej do"), 0.7f),
        new PatternEntry(Pattern.compile("(?i)(.+)dekh lena"), 0.7f)
    };

    // Date patterns - English
    private static final Pattern DATE_TOMORROW = Pattern.compile("(?i)\\b(tomorrow|tmrw)\\b");
    private static final Pattern DATE_TODAY = Pattern.compile("(?i)\\b(today|by end of day|end of day|EOD)\\b");
    private static final Pattern DATE_NEXT_WEEK = Pattern.compile("(?i)\\b(next week)\\b");
    private static final Pattern DATE_BY_DAY = Pattern.compile("(?i)\\b(by\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b");

    // Date patterns - Hindi
    private static final Pattern DATE_KAL_TAK_HINDI = Pattern.compile("कल तक");
    private static final Pattern DATE_SOMVAR_TAK = Pattern.compile("(सोमवार|मंगलवार|बुधवार|गुरुवार|शुक्रवार|शनिवार|रविवार) तक");
    private static final Pattern DATE_AGLE_HAFTE_HINDI = Pattern.compile("अगले हफ्ते");

    // Date patterns - Hinglish
    private static final Pattern DATE_KAL_TAK_HINGLISH = Pattern.compile("(?i)\\bkal tak\\b");
    private static final Pattern DATE_DAY_TAK = Pattern.compile("(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday) tak\\b");
    private static final Pattern DATE_NEXT_WEEK_TAK = Pattern.compile("(?i)\\bnext week tak\\b");
    private static final Pattern DATE_AGLE_HAFTE_HINGLISH = Pattern.compile("(?i)\\bagle hafte\\b");

    public void setTeamMembers(List<String> memberNames) {
        this.teamMemberNames = memberNames != null ? memberNames : new ArrayList<String>();
    }

    public ExtractedActionItem extractFromLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String trimmed = line.trim();

        // Try Hinglish patterns first (most specific mixed patterns)
        ExtractedActionItem item = tryPatterns(trimmed, HINGLISH_PATTERNS, Language.HINGLISH);
        if (item != null) return item;

        // Try Hindi patterns
        item = tryPatterns(trimmed, HINDI_PATTERNS, Language.HINDI);
        if (item != null) return item;

        // Try English patterns
        item = tryPatterns(trimmed, ENGLISH_PATTERNS, Language.ENGLISH);
        if (item != null) return item;

        return null;
    }

    private ExtractedActionItem tryPatterns(String text, PatternEntry[] patterns, Language language) {
        for (PatternEntry entry : patterns) {
            Matcher matcher = entry.pattern.matcher(text);
            if (matcher.find()) {
                ExtractedActionItem item = new ExtractedActionItem();
                item.rawText = text;
                item.detectedLanguage = language;
                item.confidence = entry.confidence;

                // Extract title from matched group
                String extracted = null;
                for (int g = matcher.groupCount(); g >= 1; g--) {
                    String group = matcher.group(g);
                    if (group != null && !group.trim().isEmpty()) {
                        extracted = group.trim();
                        break;
                    }
                }
                item.title = extracted != null ? cleanTitle(extracted) : cleanTitle(text);

                // Detect due date
                item.dueDate = extractDate(text);

                // Detect assignee
                item.assigneeName = detectAssignee(text);

                return item;
            }
        }
        return null;
    }

    public Long extractDate(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Check English date patterns
        if (DATE_TODAY.matcher(text).find()) {
            return cal.getTimeInMillis();
        }

        if (DATE_TOMORROW.matcher(text).find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        if (DATE_NEXT_WEEK.matcher(text).find()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        Matcher dayMatcher = DATE_BY_DAY.matcher(text);
        if (dayMatcher.find()) {
            String dayName = dayMatcher.group(2);
            return getNextDayOfWeek(dayName);
        }

        // Check Hindi date patterns
        if (DATE_KAL_TAK_HINDI.matcher(text).find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        if (DATE_AGLE_HAFTE_HINDI.matcher(text).find()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        Matcher hindiDayMatcher = DATE_SOMVAR_TAK.matcher(text);
        if (hindiDayMatcher.find()) {
            String hindiDay = hindiDayMatcher.group(1);
            String englishDay = hindiDayToEnglish(hindiDay);
            if (englishDay != null) {
                return getNextDayOfWeek(englishDay);
            }
        }

        // Check Hinglish date patterns
        if (DATE_KAL_TAK_HINGLISH.matcher(text).find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        if (DATE_NEXT_WEEK_TAK.matcher(text).find() || DATE_AGLE_HAFTE_HINGLISH.matcher(text).find()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        Matcher hinglishDayMatcher = DATE_DAY_TAK.matcher(text);
        if (hinglishDayMatcher.find()) {
            String dayName = hinglishDayMatcher.group(1);
            return getNextDayOfWeek(dayName);
        }

        return null;
    }

    private Long getNextDayOfWeek(String dayName) {
        int targetDay = dayNameToCalendarDay(dayName.toLowerCase(Locale.US));
        if (targetDay == -1) return null;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntilTarget = targetDay - currentDay;
        if (daysUntilTarget <= 0) {
            daysUntilTarget += 7;
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntilTarget);
        return cal.getTimeInMillis();
    }

    private int dayNameToCalendarDay(String dayName) {
        switch (dayName) {
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            case "sunday": return Calendar.SUNDAY;
            default: return -1;
        }
    }

    private String hindiDayToEnglish(String hindiDay) {
        if (hindiDay == null) return null;
        switch (hindiDay) {
            case "\u0938\u094B\u092E\u0935\u093E\u0930": return "monday";      // सोमवार
            case "\u092E\u0902\u0917\u0932\u0935\u093E\u0930": return "tuesday";   // मंगलवार
            case "\u092C\u0941\u0927\u0935\u093E\u0930": return "wednesday";  // बुधवार
            case "\u0917\u0941\u0930\u0941\u0935\u093E\u0930": return "thursday";   // गुरुवार
            case "\u0936\u0941\u0915\u094D\u0930\u0935\u093E\u0930": return "friday";     // शुक्रवार
            case "\u0936\u0928\u093F\u0935\u093E\u0930": return "saturday";   // शनिवार
            case "\u0930\u0935\u093F\u0935\u093E\u0930": return "sunday";     // रविवार
            default: return null;
        }
    }

    private String detectAssignee(String text) {
        if (teamMemberNames == null || teamMemberNames.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase(Locale.US);
        String[] words = lowerText.split("\\s+");

        for (String memberName : teamMemberNames) {
            String lowerMember = memberName.toLowerCase(Locale.US);
            int memberIndex = -1;

            // Find member name position in word array
            for (int i = 0; i < words.length; i++) {
                if (words[i].contains(lowerMember) || lowerMember.contains(words[i])) {
                    memberIndex = i;
                    break;
                }
            }

            if (memberIndex == -1) {
                // Try substring match in the full text
                int charIndex = lowerText.indexOf(lowerMember);
                if (charIndex == -1) continue;
                // Approximate word position
                memberIndex = lowerText.substring(0, charIndex).split("\\s+").length;
            }

            // Check if within 10 words of an action verb
            if (isNearActionVerb(words, memberIndex)) {
                return memberName;
            }
        }

        return null;
    }

    private boolean isNearActionVerb(String[] words, int memberIndex) {
        String[] actionVerbs = {"will", "should", "must", "need", "send", "do", "complete",
                "review", "check", "follow", "prepare", "submit", "finish", "create",
                "update", "fix", "resolve", "handle", "take", "make"};

        int start = Math.max(0, memberIndex - 10);
        int end = Math.min(words.length, memberIndex + 10);

        for (int i = start; i < end; i++) {
            for (String verb : actionVerbs) {
                if (words[i].equals(verb)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String cleanTitle(String title) {
        if (title == null) return "";
        // Remove leading punctuation and whitespace
        title = title.replaceAll("^[:\\s,;.\\-]+", "");
        // Remove trailing punctuation
        title = title.replaceAll("[.;,]+$", "");
        return title.trim();
    }

    private static class PatternEntry {
        final Pattern pattern;
        final float confidence;

        PatternEntry(Pattern pattern, float confidence) {
            this.pattern = pattern;
            this.confidence = confidence;
        }
    }
}
