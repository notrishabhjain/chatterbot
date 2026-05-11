package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TranscriptParser {

    private final MeetingTaskExtractor extractor;
    private final IntelligentTranscriptAnalyzer intelligentAnalyzer;

    public TranscriptParser() {
        extractor = new MeetingTaskExtractor();
        intelligentAnalyzer = new IntelligentTranscriptAnalyzer();
    }

    public void setTeamMembers(List<String> memberNames) {
        extractor.setTeamMembers(memberNames);
        intelligentAnalyzer.setTeamMembers(memberNames);
    }

    /**
     * Parses a full transcript text into a list of extracted action items.
     * Splits by newlines and sentence boundaries, detects speaker labels,
     * deduplicates by title, and returns sorted by confidence descending.
     */
    public List<MeetingTaskExtractor.ExtractedActionItem> parseTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<MeetingTaskExtractor.ExtractedActionItem> results = new ArrayList<>();

        // Split by newlines first
        String[] lines = transcript.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // Detect speaker label patterns: "Name:" or "[Name]"
            String speakerName = detectSpeaker(line);
            String content = removeSpeakerLabel(line);

            // Split by sentence boundaries if the line has multiple sentences
            String[] sentences = content.split("(?<=[.?!])\\s+");

            for (String sentence : sentences) {
                if (sentence.trim().isEmpty()) {
                    continue;
                }

                MeetingTaskExtractor.ExtractedActionItem item = extractor.extractFromLine(sentence.trim());
                if (item != null) {
                    // If speaker detected and no assignee found yet, use speaker as context
                    if (item.assigneeName == null && speakerName != null) {
                        item.assigneeName = speakerName;
                    }
                    results.add(item);
                }
            }
        }

        // Deduplicate by title similarity
        results = deduplicateItems(results);

        // Second pass: Use IntelligentTranscriptAnalyzer for additional detection
        List<MeetingTaskExtractor.ExtractedActionItem> intelligentResults =
                intelligentAnalyzer.analyze(transcript, null);

        // Merge results: add items from intelligent analysis not found by first pass
        for (MeetingTaskExtractor.ExtractedActionItem intelligentItem : intelligentResults) {
            boolean isDuplicate = false;
            for (int i = 0; i < results.size(); i++) {
                MeetingTaskExtractor.ExtractedActionItem existing = results.get(i);
                if (isSimilarTitle(intelligentItem.title, existing.title)) {
                    // Keep the one with higher confidence
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
