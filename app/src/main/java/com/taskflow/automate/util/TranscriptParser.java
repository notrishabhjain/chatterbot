package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TranscriptParser {

    private final MeetingTaskExtractor extractor;
    private final IntelligentTranscriptAnalyzer intelligentAnalyzer;
    private final TranscriptTranslator translator;
    private final ContextAwareTaskExtractor contextExtractor;
    private List<String> teamMembers;
    private String lastTranslatedText = null;
    private boolean lastContainedNonEnglish = false;

    public TranscriptParser() {
        extractor = new MeetingTaskExtractor();
        intelligentAnalyzer = new IntelligentTranscriptAnalyzer();
        translator = new TranscriptTranslator();
        contextExtractor = new ContextAwareTaskExtractor();
    }

    public void setTeamMembers(List<String> memberNames) {
        this.teamMembers = memberNames;
        extractor.setTeamMembers(memberNames);
        intelligentAnalyzer.setTeamMembers(memberNames);
    }

    /**
     * Returns the last translated text after parseTranscript() is called.
     * If the original text contained Hindi/Hinglish, this will be the English translation.
     */
    public String getLastTranslatedText() {
        return lastTranslatedText;
    }

    /**
     * Returns true if the translator detected non-English content in the last parsed transcript.
     */
    public boolean lastTranscriptContainedNonEnglish() {
        return lastContainedNonEnglish;
    }

    /**
     * Parses a full transcript text into a list of extracted action items.
     * New workflow: (1) Translate Hindi/Hinglish to English, (2) Extract tasks using
     * context-aware full-transcript analysis, (3) Fallback with IntelligentTranscriptAnalyzer,
     * (4) Merge and deduplicate results sorted by confidence descending.
     */
    public List<MeetingTaskExtractor.ExtractedActionItem> parseTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // FIRST: Detect non-English content and translate to English
        lastContainedNonEnglish = translator.containsNonEnglish(transcript);
        lastTranslatedText = translator.translateToEnglish(transcript);

        List<MeetingTaskExtractor.ExtractedActionItem> results = new ArrayList<>();

        // SECOND: Context-aware extraction on the translated text
        List<ContextAwareTaskExtractor.ExtractedTask> contextTasks =
                contextExtractor.extractTasks(lastTranslatedText, this.teamMembers);

        // THIRD: Convert ExtractedTask objects to ExtractedActionItem for compatibility
        for (ContextAwareTaskExtractor.ExtractedTask ctxTask : contextTasks) {
            MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
            item.title = ctxTask.title;
            item.assigneeName = ctxTask.assignee;
            item.dueDate = ctxTask.dueDate;
            item.confidence = ctxTask.confidence;
            item.rawText = ctxTask.rawContext != null ? ctxTask.rawContext : "";
            item.detectedLanguage = MeetingTaskExtractor.Language.ENGLISH;
            results.add(item);
        }

        // FOURTH: Fallback/supplementary pass with IntelligentTranscriptAnalyzer on translated text
        List<MeetingTaskExtractor.ExtractedActionItem> intelligentResults =
                intelligentAnalyzer.analyze(lastTranslatedText, this.teamMembers);

        // FIFTH: Merge results, deduplicating by title similarity (keep higher confidence)
        for (MeetingTaskExtractor.ExtractedActionItem intelligentItem : intelligentResults) {
            boolean isDuplicate = false;
            for (int i = 0; i < results.size(); i++) {
                MeetingTaskExtractor.ExtractedActionItem existing = results.get(i);
                if (isSimilarTitle(intelligentItem.title, existing.title)) {
                    if (intelligentItem.confidence > existing.confidence) {
                        results.set(i, intelligentItem);
                    }
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                results.add(intelligentItem);
            }
        }

        // Sort by confidence descending
        Collections.sort(results, new Comparator<MeetingTaskExtractor.ExtractedActionItem>() {
            @Override
            public int compare(MeetingTaskExtractor.ExtractedActionItem a, MeetingTaskExtractor.ExtractedActionItem b) {
                return Float.compare(b.confidence, a.confidence);
            }
        });

        return results;
    }

    private String detectSpeaker(String line) {
        String trimmed = line.trim();

        // Pattern: "Name:" at beginning
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0 && colonIndex < 30) {
            String possibleName = trimmed.substring(0, colonIndex).trim();
            // Ensure it looks like a name (no spaces beyond a first+last name scenario)
            if (possibleName.split("\\s+").length <= 3 && possibleName.matches("[A-Za-z\\s]+")) {
                return possibleName;
            }
        }

        // Pattern: "[Name]" at beginning
        if (trimmed.startsWith("[")) {
            int closeBracket = trimmed.indexOf(']');
            if (closeBracket > 1 && closeBracket < 30) {
                return trimmed.substring(1, closeBracket).trim();
            }
        }

        return null;
    }

    private String removeSpeakerLabel(String line) {
        String trimmed = line.trim();

        // Remove "Name:" prefix
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0 && colonIndex < 30) {
            String possibleName = trimmed.substring(0, colonIndex).trim();
            if (possibleName.split("\\s+").length <= 3 && possibleName.matches("[A-Za-z\\s]+")) {
                return trimmed.substring(colonIndex + 1).trim();
            }
        }

        // Remove "[Name]" prefix
        if (trimmed.startsWith("[")) {
            int closeBracket = trimmed.indexOf(']');
            if (closeBracket > 1 && closeBracket < 30) {
                return trimmed.substring(closeBracket + 1).trim();
            }
        }

        return trimmed;
    }

    private List<MeetingTaskExtractor.ExtractedActionItem> deduplicateItems(
            List<MeetingTaskExtractor.ExtractedActionItem> items) {
        List<MeetingTaskExtractor.ExtractedActionItem> deduplicated = new ArrayList<>();

        for (MeetingTaskExtractor.ExtractedActionItem item : items) {
            boolean isDuplicate = false;
            for (MeetingTaskExtractor.ExtractedActionItem existing : deduplicated) {
                if (isSimilarTitle(item.title, existing.title)) {
                    // Keep the one with higher confidence
                    if (item.confidence > existing.confidence) {
                        deduplicated.remove(existing);
                        deduplicated.add(item);
                    }
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                deduplicated.add(item);
            }
        }

        return deduplicated;
    }

    private boolean isSimilarTitle(String title1, String title2) {
        if (title1 == null || title2 == null) return false;
        String normalized1 = title1.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
        String normalized2 = title2.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();

        if (normalized1.equals(normalized2)) return true;
        if (normalized1.isEmpty() || normalized2.isEmpty()) return false;

        // Check if one contains the other
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            // Only consider it a duplicate if the shorter string is at least 70% of the longer
            int shorter = Math.min(normalized1.length(), normalized2.length());
            int longer = Math.max(normalized1.length(), normalized2.length());
            return (float) shorter / longer >= 0.7f;
        }

        return false;
    }
}
