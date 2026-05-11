package com.taskflow.automate.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class IntelligentTranscriptAnalyzerTest {

    private IntelligentTranscriptAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new IntelligentTranscriptAnalyzer();
    }

    // --- Language detection tests ---

    @Test
    public void detectLanguage_pureEnglish_returnsEnglish() {
        IntelligentTranscriptAnalyzer.Language lang =
                analyzer.detectLanguage("I will finish the report by tomorrow");
        assertEquals(IntelligentTranscriptAnalyzer.Language.ENGLISH, lang);
    }

    @Test
    public void detectLanguage_pureHindiDevanagari_returnsHindi() {
        IntelligentTranscriptAnalyzer.Language lang =
                analyzer.detectLanguage("\u092E\u0941\u091D\u0947 \u0915\u0932 \u0924\u0915 \u092F\u0939 \u092A\u0942\u0930\u093E \u0915\u0930\u0928\u093E \u0939\u0948");
        assertEquals(IntelligentTranscriptAnalyzer.Language.HINDI, lang);
    }

    @Test
    public void detectLanguage_hinglishMix_returnsHinglish() {
        IntelligentTranscriptAnalyzer.Language lang =
                analyzer.detectLanguage("mujhe presentation ready karna hai kal tak");
        assertEquals(IntelligentTranscriptAnalyzer.Language.HINGLISH, lang);
    }

    @Test
    public void detectLanguage_emptyString_returnsEnglish() {
        IntelligentTranscriptAnalyzer.Language lang = analyzer.detectLanguage("");
        assertEquals(IntelligentTranscriptAnalyzer.Language.ENGLISH, lang);
    }

    @Test
    public void detectLanguage_null_returnsEnglish() {
        IntelligentTranscriptAnalyzer.Language lang = analyzer.detectLanguage(null);
        assertEquals(IntelligentTranscriptAnalyzer.Language.ENGLISH, lang);
    }

    @Test
    public void detectLanguage_mixedDevanagariAndLatin_returnsHinglish() {
        // Devanagari mixed with Latin characters
        IntelligentTranscriptAnalyzer.Language lang =
                analyzer.detectLanguage("\u092E\u0948\u0902 report \u092D\u0947\u091C\u0942\u0902\u0917\u093E");
        assertEquals(IntelligentTranscriptAnalyzer.Language.HINGLISH, lang);
    }

    // --- Implicit task detection tests ---

    @Test
    public void detectImplicitTasks_whoWillQuestion_returnsTask() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("Who will handle the deployment?");
        assertFalse(items.isEmpty());
        assertEquals(0.5f, items.get(0).confidence, 0.01f);
    }

    @Test
    public void detectImplicitTasks_canSomeoneQuestion_returnsTask() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("Can someone review this PR?");
        assertFalse(items.isEmpty());
        assertTrue(items.get(0).title.contains("review"));
    }

    @Test
    public void detectImplicitTasks_hindiImplicitQuestion_returnsTask() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("kiska zimma hai ye release?");
        assertFalse(items.isEmpty());
    }

    @Test
    public void detectImplicitTasks_noImplicitTask_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("The weather is nice today");
        assertTrue(items.isEmpty());
    }

    @Test
    public void detectImplicitTasks_null_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks(null);
        assertTrue(items.isEmpty());
    }

    @Test
    public void detectImplicitTasks_empty_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("");
        assertTrue(items.isEmpty());
    }

    // --- Context-aware deadline linking tests ---

    @Test
    public void findNearbyDeadline_adjacentSentenceHasDeadline_returnsDate() {
        List<String> sentences = Arrays.asList(
                "We need to finish the report",
                "Deadline is tomorrow",
                "Let us discuss the budget"
        );
        Long deadline = analyzer.findNearbyDeadline(sentences, 0);
        assertNotNull(deadline);
        // Should be approximately tomorrow
        long now = System.currentTimeMillis();
        assertTrue(deadline > now);
        assertTrue(deadline < now + 48 * 60 * 60 * 1000L);
    }

    @Test
    public void findNearbyDeadline_noDeadlineNearby_returnsNull() {
        List<String> sentences = Arrays.asList(
                "The project is going well",
                "We have a great team",
                "Everything is on track"
        );
        Long deadline = analyzer.findNearbyDeadline(sentences, 1);
        assertNull(deadline);
    }

    @Test
    public void findNearbyDeadline_nullSentences_returnsNull() {
        Long deadline = analyzer.findNearbyDeadline(null, 0);
        assertNull(deadline);
    }

    @Test
    public void findNearbyDeadline_invalidIndex_returnsNull() {
        List<String> sentences = Arrays.asList("Hello", "World");
        Long deadline = analyzer.findNearbyDeadline(sentences, -1);
        assertNull(deadline);
        deadline = analyzer.findNearbyDeadline(sentences, 5);
        assertNull(deadline);
    }

    // --- Multi-speaker bilingual transcript parsing tests ---

    @Test
    public void analyze_bilingualTranscript_extractsMultipleItems() {
        String transcript =
                "Rahul: mujhe kal tak presentation ready karna hai\n" +
                "Priya: main slides design karungi\n" +
                "Rahul: Aur data team se numbers maango. Deadline friday hai.";

        List<String> members = Arrays.asList("Rahul", "Priya");
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(transcript, members);

        assertTrue("Expected at least 3 action items but got " + results.size(), results.size() >= 3);
    }

    @Test
    public void analyze_englishTranscript_extractsItems() {
        String transcript =
                "John: I will prepare the quarterly report\n" +
                "Sarah: Please review the budget by Friday\n" +
                "John: Action item: update the team wiki";

        List<String> members = Arrays.asList("John", "Sarah");
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(transcript, members);

        assertTrue("Expected at least 2 items but got " + results.size(), results.size() >= 2);
    }

    @Test
    public void analyze_hindiDevanagariTranscript_extractsItems() {
        String transcript = "\u092E\u0941\u091D\u0947 \u0930\u093F\u092A\u094B\u0930\u094D\u091F \u092D\u0947\u091C\u0928\u093E \u0939\u0948 \u0915\u0932 \u0924\u0915";
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(transcript, null);

        assertFalse(results.isEmpty());
    }

    // --- Confidence boosting tests ---

    @Test
    public void applyContextBoost_urgentKeywordPresent_boostsConfidence() {
        MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
        item.rawText = "finish the report";
        item.title = "finish the report";
        item.confidence = 0.7f;
        item.detectedLanguage = MeetingTaskExtractor.Language.ENGLISH;

        List<MeetingTaskExtractor.ExtractedActionItem> items = Arrays.asList(item);
        analyzer.applyContextBoost(items, "This is urgent, we need to finish the report asap");

        assertTrue(item.confidence > 0.7f);
    }

    @Test
    public void applyContextBoost_noUrgencyKeywords_noChange() {
        MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
        item.rawText = "review the document";
        item.title = "review the document";
        item.confidence = 0.7f;
        item.detectedLanguage = MeetingTaskExtractor.Language.ENGLISH;

        List<MeetingTaskExtractor.ExtractedActionItem> items = Arrays.asList(item);
        analyzer.applyContextBoost(items, "please review the document when you get a chance");

        assertEquals(0.7f, item.confidence, 0.001f);
    }

    @Test
    public void applyContextBoost_hindiUrgencyKeyword_boostsConfidence() {
        MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
        item.rawText = "report bhejo";
        item.title = "report bhejo";
        item.confidence = 0.7f;
        item.detectedLanguage = MeetingTaskExtractor.Language.HINGLISH;

        List<MeetingTaskExtractor.ExtractedActionItem> items = Arrays.asList(item);
        analyzer.applyContextBoost(items, "jaldi report bhejo ye zaroori hai");

        assertTrue(item.confidence > 0.7f);
    }

    @Test
    public void applyContextBoost_confidenceCapAt1() {
        MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
        item.rawText = "fix this";
        item.title = "fix this";
        item.confidence = 0.95f;
        item.detectedLanguage = MeetingTaskExtractor.Language.ENGLISH;

        List<MeetingTaskExtractor.ExtractedActionItem> items = Arrays.asList(item);
        analyzer.applyContextBoost(items, "urgent critical asap immediately fix this");

        assertTrue(item.confidence <= 1.0f);
    }

    @Test
    public void applyContextBoost_nullItems_noException() {
        analyzer.applyContextBoost(null, "some text");
        // Should not throw
    }

    @Test
    public void applyContextBoost_nullText_noException() {
        MeetingTaskExtractor.ExtractedActionItem item = new MeetingTaskExtractor.ExtractedActionItem();
        item.confidence = 0.5f;
        List<MeetingTaskExtractor.ExtractedActionItem> items = Arrays.asList(item);
        analyzer.applyContextBoost(items, null);
        assertEquals(0.5f, item.confidence, 0.001f);
    }

    // --- Sentence splitting tests ---

    @Test
    public void splitIntoSentences_englishText_splitsByPeriod() {
        List<String> sentences = analyzer.splitIntoSentences("First task. Second task. Third task.");
        assertTrue(sentences.size() >= 3);
    }

    @Test
    public void splitIntoSentences_hindiDanda_splitsCorrectly() {
        // Devanagari Danda is the Hindi sentence terminator
        List<String> sentences = analyzer.splitIntoSentences(
                "\u092A\u0939\u0932\u093E \u0915\u093E\u092E\u0964 \u0926\u0942\u0938\u0930\u093E \u0915\u093E\u092E");
        assertTrue(sentences.size() >= 2);
    }

    @Test
    public void splitIntoSentences_multiLine_splitsCorrectly() {
        List<String> sentences = analyzer.splitIntoSentences("Line one\nLine two\nLine three");
        assertEquals(3, sentences.size());
    }

    @Test
    public void splitIntoSentences_null_returnsEmpty() {
        List<String> sentences = analyzer.splitIntoSentences(null);
        assertTrue(sentences.isEmpty());
    }

    @Test
    public void splitIntoSentences_empty_returnsEmpty() {
        List<String> sentences = analyzer.splitIntoSentences("");
        assertTrue(sentences.isEmpty());
    }

    // --- Edge cases ---

    @Test
    public void analyze_nullInput_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void analyze_emptyInput_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze("", null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void analyze_singleWord_handlesGracefully() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze("hello", null);
        assertNotNull(results);
        // May or may not extract items, but should not crash
    }

    @Test
    public void analyze_whitespaceOnly_returnsEmpty() {
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze("   \n  \n  ", null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void analyze_longTranscript_handlesWithoutError() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Speaker ").append(i).append(": I need to finish task ").append(i).append("\n");
        }
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(sb.toString(), null);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    // --- Integration: team member detection ---

    @Test
    public void analyze_withTeamMembers_detectsAssignees() {
        String transcript = "Rahul: I will prepare the slides\nPriya: I need to review the design";
        List<String> members = Arrays.asList("Rahul", "Priya");
        List<MeetingTaskExtractor.ExtractedActionItem> results = analyzer.analyze(transcript, members);

        assertFalse(results.isEmpty());
    }

    @Test
    public void analyze_implicitTaskWithTeamMember_detectsMember() {
        analyzer.setTeamMembers(Arrays.asList("Rahul", "Priya"));
        List<MeetingTaskExtractor.ExtractedActionItem> items =
                analyzer.detectImplicitTasks("Who will handle Rahul's task?");
        if (!items.isEmpty()) {
            assertEquals("Rahul", items.get(0).assigneeName);
        }
    }
}
