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

    // --- Enhanced capability tests ---

    @Test
    public void fullHinglishTranscript_multiSpeaker_extractsTasks() {
        parser.setTeamMembers(Arrays.asList("Rahul", "Priya", "Amit"));
        String transcript =
                "Rahul: mujhe kal tak presentation ready karna hai\n" +
                "Priya: main slides design karungi\n" +
                "Amit: data team se numbers bhej do\n" +
                "Rahul: deadline friday hai, jaldi karo";

        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);

        assertTrue("Expected at least 3 items but got " + results.size(), results.size() >= 3);
    }

    @Test
    public void hindiDevanagariTranscript_extractsItems() {
        String transcript = "\u092E\u0941\u091D\u0947 \u0930\u093F\u092A\u094B\u0930\u094D\u091F \u092D\u0947\u091C\u0928\u093E \u0939\u0948 \u0915\u0932 \u0924\u0915";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);

        assertFalse("Should extract at least one item from Hindi text", results.isEmpty());
    }

    @Test
    public void implicitTasksDetected_whoWillQuestion() {
        String transcript = "Great progress on the project.\nWho will handle the deployment?\nLet us move on.";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);

        // The intelligent analyzer should detect the implicit task
        boolean foundImplicit = false;
        for (MeetingTaskExtractor.ExtractedActionItem item : results) {
            if (item.title != null && item.title.toLowerCase().contains("deployment")) {
                foundImplicit = true;
                break;
            }
        }
        assertTrue("Should detect implicit task from question", foundImplicit);
    }

    @Test
    public void backwardCompatibility_existingTestsStillWork() {
        // Ensure basic English parsing still works after integration
        String transcript = "I will prepare the presentation\nAction item: review the budget";
        List<MeetingTaskExtractor.ExtractedActionItem> results =
                parser.parseTranscript(transcript);
        assertTrue(results.size() >= 2);
    }

    @Test
    public void backwardCompatibility_nullStillSafe() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = parser.parseTranscript(null);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void backwardCompatibility_emptyStillSafe() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = parser.parseTranscript("");
        assertNotNull(results);
        assertEquals(0, results.size());
    }
}
