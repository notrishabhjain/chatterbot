package com.taskflow.automate.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TranscriptParserTest {

    private TranscriptParser parser;

    @Before
    public void setUp() {
        parser = new TranscriptParser();
    }

    @Test
    public void singleLineWithActionItem_returnsOneItem() {
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript("I will prepare the presentation");
        assertEquals(1, results.size());
    }

    @Test
    public void multipleLines_returnsMultipleItems() {
        String transcript = "I will do the report\nWe need to review the code\nAction item: update the wiki";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        assertTrue(results.size() >= 2);
    }

    @Test
    public void noActionItems_returnsEmptyList() {
        String transcript = "Great meeting everyone\nSee you next time\nThanks!";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        assertEquals(0, results.size());
    }

    @Test
    public void withSpeakerLabel_detectsAssignee() {
        parser.setTeamMembers(Arrays.asList("John", "Sarah"));
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript("John: I will review the code");
        assertFalse(results.isEmpty());
        assertEquals("John", results.get(0).assigneeName);
    }

    @Test
    public void withBracketSpeakerLabel_detectsAssignee() {
        parser.setTeamMembers(Arrays.asList("Sarah", "Mike"));
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript("[Sarah] I need to update the docs");
        assertFalse(results.isEmpty());
        assertEquals("Sarah", results.get(0).assigneeName);
    }

    @Test
    public void emptyInput_returnsEmptyList() {
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript("");
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void nullInput_returnsEmptyList() {
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(null);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void duplicateItems_deduplicates() {
        String transcript = "I will send the report\nI will send the report\nI will send the report";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        assertTrue(results.size() < 3);
    }

    @Test
    public void mixedLanguages_detectsAll() {
        String transcript = "I will send the email\nmujhe report send karna hai";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        assertTrue(results.size() >= 2);
        // Should have items from different languages
        boolean hasEnglish = false;
        boolean hasHinglish = false;
        for (MeetingTaskExtractor.ExtractedActionItem item : results) {
            if (item.detectedLanguage == MeetingTaskExtractor.Language.ENGLISH) hasEnglish = true;
            if (item.detectedLanguage == MeetingTaskExtractor.Language.HINGLISH) hasHinglish = true;
        }
        assertTrue(hasEnglish);
        assertTrue(hasHinglish);
    }

    @Test
    public void resultsSortedByConfidenceDescending() {
        String transcript = "We should think about this\nI will complete the report\nAction item: send email";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        if (results.size() >= 2) {
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).confidence >= results.get(i + 1).confidence);
            }
        }
    }
}
