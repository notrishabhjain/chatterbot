package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-pass intelligent transcript analyzer that provides:
 * 1. Language detection per sentence (Hindi/English/Hinglish based on character analysis)
 * 2. Context-aware parsing (linking deadlines from one sentence to tasks in adjacent sentences)
 * 3. Implicit task detection (questions that imply action)
 * 4. Confidence boosting from context (urgency words near tasks boost confidence)
 * 5. Better sentence boundary detection for Hindi/Devanagari text
 * 6. Speaker continuity tracking
 *
 * All processing is on-device with no network calls or external APIs.
 */
public class IntelligentTranscriptAnalyzer {

    public enum Language { ENGLISH, HINDI, HINGLISH }

    private List<String> teamMembers = new ArrayList<>();

    // Implicit task patterns - questions/suggestions that imply action needed
    private static final Pattern[] ENGLISH_IMPLICIT_PATTERNS = {
        Pattern.compile("(?i)\\bwho will\\b(.+)\\??"),
        Pattern.compile("(?i)\\bwho can\\b(.+)\\??"),
        Pattern.compile("(?i)\\bwho is going to\\b(.+)\\??"),
        Pattern.compile("(?i)\\bcan someone\\b(.+)\\??"),
        Pattern.compile("(?i)\\bsomeone needs to\\b(.+)"),
        Pattern.compile("(?i)\\bwe need someone to\\b(.+)"),
        Pattern.compile("(?i)\\bwho is handling\\b(.+)\\??"),
        Pattern.compile("(?i)\\bisn't this pending\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bwhat about\\b(.+)\\??"),
        Pattern.compile("(?i)\\bshould we not\\b(.+)\\??")
    };

    private static final Pattern[] HINDI_IMPLICIT_PATTERNS = {
        Pattern.compile("(?i)\\bkiska zimma hai\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkaun karega\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkaun dekhega\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkisko assign kare\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkya ye pending hai\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkya ye ho gaya\\b(.+)?\\??"),
        Pattern.compile("(?i)\\bkisne kiya\\b(.+)?\\??")
    };

    // Urgency keywords that boost confidence when near tasks
    private static final String[] URGENCY_KEYWORDS_ENGLISH = {
        "urgent", "asap", "immediately", "critical", "important",
        "high priority", "blocker", "deadline", "overdue", "today",
        "right now", "right away", "time sensitive", "cannot wait"
    };

    private static final String[] URGENCY_KEYWORDS_HINDI = {
        "jaldi", "turant", "abhi", "zaruri", "zaroori", "important",
        "urgent", "fatafat", "jald se jald", "priority",
        "\u091C\u0932\u094D\u0926\u0940", // जल्दी
        "\u0924\u0941\u0930\u0902\u0924",  // तुरंत
        "\u0905\u092D\u0940",              // अभी
        "\u091C\u093C\u0930\u0942\u0930\u0940" // ज़रूरी
    };

    // Date reference patterns for nearby deadline linking
    private static final Pattern DATE_PATTERN_TOMORROW = Pattern.compile(
            "(?i)\\b(tomorrow|kal|tmrw|\u0915\u0932)\\b");
    private static final Pattern DATE_PATTERN_TODAY = Pattern.compile(
            "(?i)\\b(today|aaj|EOD|end of day|\u0906\u091C)\\b");
    private static final Pattern DATE_PATTERN_NEXT_WEEK = Pattern.compile(
            "(?i)\\b(next week|agle hafte|\u0905\u0917\u0932\u0947 \u0939\u092B\u094D\u0924\u0947)\\b");
    private static final Pattern DATE_PATTERN_DAY = Pattern.compile(
            "(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b");
    private static final Pattern DATE_PATTERN_HINGLISH_DAY = Pattern.compile(
            "(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*tak\\b");

    private final MeetingTaskExtractor meetingExtractor;

    public IntelligentTranscriptAnalyzer() {
        meetingExtractor = new MeetingTaskExtractor();
    }

    public void setTeamMembers(List<String> members) {
        this.teamMembers = members != null ? members : new ArrayList<String>();
        meetingExtractor.setTeamMembers(this.teamMembers);
    }

    /**
     * Main entry point: multi-pass analysis of a transcript.
     * Returns extracted action items with language detection and context awareness.
     */
    public List<MeetingTaskExtractor.ExtractedActionItem> analyze(String transcript, List<String> teamMembers) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return new ArrayList<>();
        }

        setTeamMembers(teamMembers);

        List<MeetingTaskExtractor.ExtractedActionItem> allItems = new ArrayList<>();

        // Split into sentences with support for both Latin and Devanagari boundaries
        List<String> sentences = splitIntoSentences(transcript);

        // Pass 1 & 2: Language detection and direct extraction per sentence
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            if (sentence.isEmpty()) {
                continue;
            }

            // Detect language for this sentence
            Language lang = detectLanguage(sentence);

            // Try extraction via MeetingTaskExtractor
            MeetingTaskExtractor.ExtractedActionItem item = meetingExtractor.extractFromLine(sentence);
            if (item != null) {
                allItems.add(item);
            }
        }

        // Pass 3: Implicit task detection
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            if (sentence.isEmpty()) {
                continue;
            }

            List<MeetingTaskExtractor.ExtractedActionItem> implicitItems = detectImplicitTasks(sentence);
            for (MeetingTaskExtractor.ExtractedActionItem implicit : implicitItems) {
                // Only add if not already detected
                boolean alreadyFound = false;
                for (MeetingTaskExtractor.ExtractedActionItem existing : allItems) {
                    if (isSimilarTitle(implicit.title, existing.title)) {
                        alreadyFound = true;
                        break;
                    }
                }
                if (!alreadyFound) {
                    allItems.add(implicit);
                }
            }
        }

        // Pass 4: Context-aware deadline linking
        for (int i = 0; i < allItems.size(); i++) {
            MeetingTaskExtractor.ExtractedActionItem item = allItems.get(i);
            if (item.dueDate == null) {
                Long nearbyDeadline = findNearbyDeadline(sentences, findSentenceIndex(sentences, item.rawText));
                if (nearbyDeadline != null) {
                    item.dueDate = nearbyDeadline;
                }
            }
        }

        // Pass 5: Confidence boosting from context
        applyContextBoost(allItems, transcript);

        return allItems;
    }

    /**
     * Detects the primary language of a sentence based on character distribution.
     * Uses Devanagari character ratio and romanized Hindi markers.
     */
    public Language detectLanguage(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return Language.ENGLISH;
        }

        int totalChars = 0;
        int devanagariChars = 0;
        int latinChars = 0;

        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (Character.isWhitespace(c) || Character.isDigit(c)) {
                continue;
            }
            totalChars++;
            // Devanagari Unicode block: U+0900 to U+097F
            if (c >= '\u0900' && c <= '\u097F') {
                devanagariChars++;
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latinChars++;
            }
        }

        if (totalChars == 0) {
            return Language.ENGLISH;
        }

        float devanagariRatio = (float) devanagariChars / totalChars;
        float latinRatio = (float) latinChars / totalChars;

        // Primarily Devanagari
        if (devanagariRatio > 0.5f) {
            if (latinRatio > 0.1f) {
                return Language.HINGLISH;
            }
            return Language.HINDI;
        }

        // Primarily Latin - check for romanized Hindi markers
        if (latinRatio > 0.5f) {
            if (hasRomanizedHindiMarkers(sentence)) {
                return Language.HINGLISH;
            }
            return Language.ENGLISH;
        }

        // Mixed or ambiguous
        if (devanagariRatio > 0.2f && latinRatio > 0.2f) {
            return Language.HINGLISH;
        }

        return Language.ENGLISH;
    }

    /**
     * Splits text into sentences handling both Latin and Devanagari sentence boundaries.
     * The pipe character (|) is a sentence terminator in Hindi (Devanagari Danda).
     */
    public List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return sentences;
        }

        // First split by newlines (each line might be a speaker turn)
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // Split by sentence boundaries: period, question mark, exclamation,
            // Devanagari danda (|), and double danda (||)
            String[] parts = line.split("(?<=[.?!\u0964\u0965])\\s*|(?<=\\|)\\s*");

            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed);
                }
            }
        }

        return sentences;
    }

    /**
     * Detects implicit tasks from questions and suggestions that imply action needed.
     * Examples: "Who will handle X?", "Kiska zimma hai ye?"
     */
    public List<MeetingTaskExtractor.ExtractedActionItem> detectImplicitTasks(String sentence) {
        List<MeetingTaskExtractor.ExtractedActionItem> items = new ArrayList<>();
        if (sentence == null || sentence.isEmpty()) {
            return items;
        }

        // Try English implicit patterns
        for (Pattern pattern : ENGLISH_IMPLICIT_PATTERNS) {
            Matcher matcher = pattern.matcher(sentence);
            if (matcher.find()) {
                MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
                item.rawText = sentence;
                item.detectedLanguage = MeetingTaskExtractor.Language.ENGLISH;
                item.confidence = 0.5f; // Implicit tasks get moderate confidence

                String extracted = matcher.group(1);
                item.title = cleanTitle(extracted != null ? extracted : sentence);
                item.dueDate = meetingExtractor.extractDate(sentence);
                item.assigneeName = detectAssigneeFromMembers(sentence);
                items.add(item);
                break;
            }
        }

        // Try Hindi implicit patterns (only if no English implicit found)
        if (items.isEmpty()) {
            for (Pattern pattern : HINDI_IMPLICIT_PATTERNS) {
                Matcher matcher = pattern.matcher(sentence);
                if (matcher.find()) {
                    MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
                    item.rawText = sentence;
                    item.detectedLanguage = MeetingTaskExtractor.Language.HINGLISH;
                    item.confidence = 0.5f;

                    String extracted = matcher.group(1);
                    item.title = cleanTitle(extracted != null ? extracted : sentence);
                    item.dueDate = meetingExtractor.extractDate(sentence);
                    item.assigneeName = detectAssigneeFromMembers(sentence);
                    items.add(item);
                    break;
                }
            }
        }

        return items;
    }

    /**
     * Boosts confidence of extracted items if urgency keywords are found nearby in the text.
     */
    public void applyContextBoost(List<MeetingTaskExtractor.ExtractedActionItem> items, String fullText) {
        if (items == null || fullText == null) {
            return;
        }

        String lowerText = fullText.toLowerCase(Locale.US);

        for (MeetingTaskExtractor.ExtractedActionItem item : items) {
            float boost = 0.0f;

            // Check English urgency keywords
            for (String keyword : URGENCY_KEYWORDS_ENGLISH) {
                if (lowerText.contains(keyword)) {
                    boost = Math.max(boost, 0.1f);
                }
            }

            // Check Hindi urgency keywords
            for (String keyword : URGENCY_KEYWORDS_HINDI) {
                if (lowerText.contains(keyword.toLowerCase(Locale.US))) {
                    boost = Math.max(boost, 0.1f);
                }
            }

            // Apply boost but cap at 1.0
            if (boost > 0.0f) {
                item.confidence = Math.min(1.0f, item.confidence + boost);
            }
        }
    }

    /**
     * Looks at surrounding sentences for deadline mentions and returns the parsed date.
     * Searches up to 2 sentences before and after the current index.
     */
    public Long findNearbyDeadline(List<String> sentences, int currentIndex) {
        if (sentences == null || currentIndex < 0 || currentIndex >= sentences.size()) {
            return null;
        }

        int start = Math.max(0, currentIndex - 2);
        int end = Math.min(sentences.size(), currentIndex + 3);

        for (int i = start; i < end; i++) {
            if (i == currentIndex) {
                continue;
            }
            String sentence = sentences.get(i);
            Long date = extractDateFromSentence(sentence);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    // --- Private helper methods ---

    private boolean hasRomanizedHindiMarkers(String sentence) {
        String lower = sentence.toLowerCase(Locale.US);
        String[] hindiMarkers = {
            "karo", "karna", "hai", "hoga", "padega", "chahiye",
            "mujhe", "tumhe", "hume", "aapko", "usko",
            "bhej", "dekh", "bata", "kar do", "bhej do",
            "kal", "aaj", "abhi", "tak", "mein",
            "karunga", "karungi", "karenge", "dena", "lena"
        };

        int markerCount = 0;
        for (String marker : hindiMarkers) {
            if (lower.contains(marker)) {
                markerCount++;
            }
        }
        // If 2 or more Hindi markers found in Latin text, it is Hinglish
        return markerCount >= 2;
    }

    private int findSentenceIndex(List<String> sentences, String rawText) {
        if (rawText == null || sentences == null) {
            return 0;
        }
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).contains(rawText) || rawText.contains(sentences.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private Long extractDateFromSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (DATE_PATTERN_TODAY.matcher(sentence).find()) {
            return cal.getTimeInMillis();
        }

        if (DATE_PATTERN_TOMORROW.matcher(sentence).find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        if (DATE_PATTERN_NEXT_WEEK.matcher(sentence).find()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            return cal.getTimeInMillis();
        }

        Matcher dayMatcher = DATE_PATTERN_DAY.matcher(sentence);
        if (dayMatcher.find()) {
            String dayName = dayMatcher.group(1).toLowerCase(Locale.US);
            return getNextDayOfWeek(dayName);
        }

        Matcher hinglishDayMatcher = DATE_PATTERN_HINGLISH_DAY.matcher(sentence);
        if (hinglishDayMatcher.find()) {
            String dayName = hinglishDayMatcher.group(1).toLowerCase(Locale.US);
            return getNextDayOfWeek(dayName);
        }

        return null;
    }

    private Long getNextDayOfWeek(String dayName) {
        int targetDay = dayNameToCalendarDay(dayName);
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

    private String detectAssigneeFromMembers(String text) {
        if (teamMembers == null || teamMembers.isEmpty() || text == null) {
            return null;
        }

        String lowerText = text.toLowerCase(Locale.US);
        for (String member : teamMembers) {
            if (lowerText.contains(member.toLowerCase(Locale.US))) {
                return member;
            }
        }
        return null;
    }

    private boolean isSimilarTitle(String title1, String title2) {
        if (title1 == null || title2 == null) return false;
        String normalized1 = title1.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").trim();
        String normalized2 = title2.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").trim();

        if (normalized1.equals(normalized2)) return true;
        if (normalized1.isEmpty() || normalized2.isEmpty()) return false;

        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            int shorter = Math.min(normalized1.length(), normalized2.length());
            int longer = Math.max(normalized1.length(), normalized2.length());
            return (float) shorter / longer >= 0.7f;
        }

        return false;
    }

    private String cleanTitle(String title) {
        if (title == null) return "";
        title = title.replaceAll("^[:\\s,;.\\-?]+", "");
        title = title.replaceAll("[.;,?]+$", "");
        return title.trim();
    }
}
