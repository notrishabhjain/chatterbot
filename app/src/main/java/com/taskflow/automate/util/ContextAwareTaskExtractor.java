package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts tasks from a full translated English transcript using cross-sentence context.
 * Instead of analyzing each sentence independently, this class uses a sliding window
 * to correlate action verbs, assignees, and deadlines across multiple sentences.
 */
public class ContextAwareTaskExtractor {

    /**
     * Represents a task extracted from the transcript with context-aware information.
     */
    public static class ExtractedTask {
        public String title;
        public String assignee;
        public Long dueDate;
        public float confidence;
        public String rawContext;
    }

    // Action patterns for English text (since input is now translated)
    private static final Pattern[] ACTION_PATTERNS = {
        Pattern.compile("(?i)\\b(will)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(need to|needs to)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(have to|has to)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(must)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(should)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(please)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(make sure|ensure)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(assigned to)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(responsible for)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(action item)\\b[:\\s]*(.+)?"),
        Pattern.compile("(?i)\\b(TODO)\\b[:\\s]*(.+)?"),
        Pattern.compile("(?i)\\b(follow up)\\b\\s*(.+)?"),
        Pattern.compile("(?i)\\b(take care of)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(prepare)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(complete)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(send)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(review)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(check)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(submit)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(deliver by)\\b\\s+(.+)"),
        Pattern.compile("(?i)\\b(deadline)\\b[:\\s]*(.+)?")
    };

    // Date extraction patterns
    private static final Pattern DATE_TOMORROW = Pattern.compile("(?i)\\b(tomorrow|by tomorrow)\\b");
    private static final Pattern DATE_TODAY = Pattern.compile("(?i)\\b(today|by today|end of day|EOD)\\b");
    private static final Pattern DATE_NEXT_WEEK = Pattern.compile("(?i)\\b(next week|by next week)\\b");
    private static final Pattern DATE_BY_DAY = Pattern.compile("(?i)\\b(by\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b");
    private static final Pattern DATE_IN_DAYS = Pattern.compile("(?i)\\bin\\s+(\\d+)\\s+days?\\b");

    // Speaker label pattern
    private static final Pattern SPEAKER_PATTERN = Pattern.compile("^([A-Za-z]+(?:\\s[A-Za-z]+)?)\\s*:");

    /**
     * Extracts tasks from the full translated English transcript using cross-sentence context.
     *
     * @param translatedTranscript The full transcript already translated to English.
     * @param teamMembers List of known team member names for assignee detection.
     * @return List of ExtractedTask sorted by confidence descending.
     */
    public List<ExtractedTask> extractTasks(String translatedTranscript, List<String> teamMembers) {
        if (translatedTranscript == null || translatedTranscript.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sentences = splitIntoSentences(translatedTranscript);
        List<ExtractedTask> tasks = new ArrayList<>();

        // Build context model for each sentence
        List<SentenceContext> contexts = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            contexts.add(buildSentenceContext(sentences.get(i), teamMembers));
        }

        // Use sliding window to extract tasks with cross-sentence correlation
        for (int i = 0; i < sentences.size(); i++) {
            SentenceContext ctx = contexts.get(i);
            if (!ctx.hasAction) {
                continue;
            }

            ExtractedTask task = new ExtractedTask();

            // Extract the core action phrase as the title
            task.title = extractActionTitle(sentences.get(i));
            if (task.title == null || task.title.trim().isEmpty()) {
                continue;
            }

            // Build raw context from the window
            int windowStart = Math.max(0, i - 2);
            int windowEnd = Math.min(sentences.size() - 1, i + 2);
            StringBuilder rawCtx = new StringBuilder();
            for (int j = windowStart; j <= windowEnd; j++) {
                if (j > windowStart) rawCtx.append(" ");
                rawCtx.append(sentences.get(j));
            }
            task.rawContext = rawCtx.toString();

            // Find assignee from the window context
            task.assignee = findAssigneeInWindow(contexts, i, windowStart, windowEnd, sentences, teamMembers);

            // Find deadline from the window context
            task.dueDate = findDeadlineInWindow(sentences, i, windowStart, windowEnd);

            // Compute confidence score
            task.confidence = computeConfidence(sentences.get(i), task.assignee, task.dueDate, ctx);

            tasks.add(task);
        }

        // Deduplicate by word overlap
        tasks = deduplicateTasks(tasks);

        // Sort by confidence descending
        Collections.sort(tasks, new Comparator<ExtractedTask>() {
            @Override
            public int compare(ExtractedTask a, ExtractedTask b) {
                return Float.compare(b.confidence, a.confidence);
            }
        });

        return tasks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return sentences;
        }

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("(?<=[.?!])\\s+");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed);
                }
            }
        }
        return sentences;
    }

    private SentenceContext buildSentenceContext(String sentence, List<String> teamMembers) {
        SentenceContext ctx = new SentenceContext();
        String lower = sentence.toLowerCase(Locale.US);

        // Check for action verbs
        String[] actionKeywords = {"will", "need to", "have to", "must", "should",
                "please", "make sure", "ensure", "assigned to", "responsible for",
                "action item", "todo", "follow up", "take care of", "prepare",
                "complete", "send", "review", "check", "submit", "deliver",
                "deadline", "handle", "finish", "create", "update", "fix", "resolve"};
        for (String keyword : actionKeywords) {
            if (lower.contains(keyword)) {
                ctx.hasAction = true;
                break;
            }
        }

        // Check for person names
        if (teamMembers != null) {
            for (String member : teamMembers) {
                if (lower.contains(member.toLowerCase(Locale.US))) {
                    ctx.personNames.add(member);
                }
            }
        }

        // Check for speaker label
        Matcher speakerMatcher = SPEAKER_PATTERN.matcher(sentence);
        if (speakerMatcher.find()) {
            ctx.speaker = speakerMatcher.group(1);
        }

        // Check for time references
        if (DATE_TOMORROW.matcher(lower).find() || DATE_TODAY.matcher(lower).find()
                || DATE_NEXT_WEEK.matcher(lower).find() || DATE_BY_DAY.matcher(lower).find()
                || DATE_IN_DAYS.matcher(lower).find()) {
            ctx.hasTimeReference = true;
        }

        return ctx;
    }

    private String extractActionTitle(String sentence) {
        // Remove speaker label if present
        Matcher speakerMatcher = SPEAKER_PATTERN.matcher(sentence);
        String content = sentence;
        if (speakerMatcher.find()) {
            content = sentence.substring(speakerMatcher.end()).trim();
        }

        // Try to extract the core action phrase from patterns
        for (Pattern pattern : ACTION_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String extracted = null;
                for (int g = matcher.groupCount(); g >= 1; g--) {
                    String group = matcher.group(g);
                    if (group != null && !group.trim().isEmpty()
                            && !group.equalsIgnoreCase("will")
                            && !group.equalsIgnoreCase("need to")
                            && !group.equalsIgnoreCase("needs to")
                            && !group.equalsIgnoreCase("have to")
                            && !group.equalsIgnoreCase("has to")
                            && !group.equalsIgnoreCase("must")
                            && !group.equalsIgnoreCase("should")
                            && !group.equalsIgnoreCase("please")
                            && !group.equalsIgnoreCase("make sure")
                            && !group.equalsIgnoreCase("ensure")
                            && !group.equalsIgnoreCase("assigned to")
                            && !group.equalsIgnoreCase("responsible for")
                            && !group.equalsIgnoreCase("action item")
                            && !group.equalsIgnoreCase("TODO")
                            && !group.equalsIgnoreCase("follow up")
                            && !group.equalsIgnoreCase("take care of")
                            && !group.equalsIgnoreCase("prepare")
                            && !group.equalsIgnoreCase("complete")
                            && !group.equalsIgnoreCase("send")
                            && !group.equalsIgnoreCase("review")
                            && !group.equalsIgnoreCase("check")
                            && !group.equalsIgnoreCase("submit")
                            && !group.equalsIgnoreCase("deliver by")
                            && !group.equalsIgnoreCase("deadline")) {
                        extracted = group.trim();
                        break;
                    }
                }
                if (extracted != null && !extracted.isEmpty()) {
                    return cleanTitle(extracted);
                }
            }
        }

        // Fallback: use the sentence content as the title
        return cleanTitle(content);
    }

    private String findAssigneeInWindow(List<SentenceContext> contexts, int currentIdx,
                                        int windowStart, int windowEnd,
                                        List<String> sentences, List<String> teamMembers) {
        if (teamMembers == null || teamMembers.isEmpty()) {
            return null;
        }

        // First: check the current sentence for direct assignment
        SentenceContext currentCtx = contexts.get(currentIdx);
        if (!currentCtx.personNames.isEmpty()) {
            return currentCtx.personNames.get(0);
        }

        // Check speaker of the current sentence
        if (currentCtx.speaker != null) {
            for (String member : teamMembers) {
                if (member.equalsIgnoreCase(currentCtx.speaker)) {
                    return member;
                }
            }
        }

        // Second: look at the window for assignee information
        for (int i = windowStart; i <= windowEnd; i++) {
            if (i == currentIdx) continue;
            SentenceContext ctx = contexts.get(i);
            if (!ctx.personNames.isEmpty()) {
                return ctx.personNames.get(0);
            }
            if (ctx.speaker != null) {
                for (String member : teamMembers) {
                    if (member.equalsIgnoreCase(ctx.speaker)) {
                        return member;
                    }
                }
            }
        }

        return null;
    }

    private Long findDeadlineInWindow(List<String> sentences, int currentIdx,
                                      int windowStart, int windowEnd) {
        // First check current sentence
        Long date = extractDateFromText(sentences.get(currentIdx));
        if (date != null) {
            return date;
        }

        // Then check the window
        for (int i = windowStart; i <= windowEnd; i++) {
            if (i == currentIdx) continue;
            date = extractDateFromText(sentences.get(i));
            if (date != null) {
                return date;
            }
        }

        return null;
    }

    private Long extractDateFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

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

        Matcher inDaysMatcher = DATE_IN_DAYS.matcher(text);
        if (inDaysMatcher.find()) {
            try {
                int days = Integer.parseInt(inDaysMatcher.group(1));
                cal.add(Calendar.DAY_OF_YEAR, days);
                return cal.getTimeInMillis();
            } catch (NumberFormatException e) {
                // ignore
            }
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

    private float computeConfidence(String sentence, String assignee, Long dueDate,
                                    SentenceContext ctx) {
        String lower = sentence.toLowerCase(Locale.US);
        float confidence = 0.5f;

        // Direct assignments get high confidence
        if (lower.contains("will") && assignee != null) {
            confidence = 0.9f;
        } else if (lower.contains("assigned to") || lower.contains("responsible for")) {
            confidence = 0.9f;
        } else if (lower.contains("action item") || lower.contains("todo")) {
            confidence = 0.9f;
        } else if (lower.contains("need to") || lower.contains("have to") || lower.contains("must")) {
            confidence = 0.8f;
        } else if (lower.contains("should") || lower.contains("please")) {
            confidence = 0.7f;
        } else if (lower.contains("make sure") || lower.contains("ensure")) {
            confidence = 0.7f;
        }

        // Contextual inference (no direct assignment) gets lower confidence
        if (assignee == null && confidence > 0.6f) {
            confidence -= 0.1f;
        }

        // Having a deadline boosts confidence slightly
        if (dueDate != null) {
            confidence = Math.min(1.0f, confidence + 0.05f);
        }

        return confidence;
    }

    private List<ExtractedTask> deduplicateTasks(List<ExtractedTask> tasks) {
        List<ExtractedTask> deduplicated = new ArrayList<>();

        for (ExtractedTask task : tasks) {
            boolean isDuplicate = false;
            for (int i = 0; i < deduplicated.size(); i++) {
                ExtractedTask existing = deduplicated.get(i);
                if (computeWordOverlap(task.title, existing.title) > 0.7f) {
                    // Keep higher confidence one
                    if (task.confidence > existing.confidence) {
                        deduplicated.set(i, task);
                    }
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                deduplicated.add(task);
            }
        }

        return deduplicated;
    }

    private float computeWordOverlap(String title1, String title2) {
        if (title1 == null || title2 == null) return 0.0f;

        String[] words1 = title1.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        String[] words2 = title2.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        if (words1.length == 0 || words2.length == 0) return 0.0f;

        Set<String> set1 = new HashSet<>();
        for (String w : words1) {
            if (!w.isEmpty()) set1.add(w);
        }
        Set<String> set2 = new HashSet<>();
        for (String w : words2) {
            if (!w.isEmpty()) set2.add(w);
        }

        if (set1.isEmpty() || set2.isEmpty()) return 0.0f;

        int intersection = 0;
        for (String w : set1) {
            if (set2.contains(w)) {
                intersection++;
            }
        }

        int smaller = Math.min(set1.size(), set2.size());
        return (float) intersection / smaller;
    }

    private String cleanTitle(String title) {
        if (title == null) return "";
        title = title.replaceAll("^[:\\s,;.\\-]+", "");
        title = title.replaceAll("[.;,]+$", "");
        // Capitalize first letter
        title = title.trim();
        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }
        return title;
    }

    /**
     * Internal context model for a sentence.
     */
    private static class SentenceContext {
        boolean hasAction = false;
        List<String> personNames = new ArrayList<>();
        String speaker = null;
        boolean hasTimeReference = false;
    }
}
