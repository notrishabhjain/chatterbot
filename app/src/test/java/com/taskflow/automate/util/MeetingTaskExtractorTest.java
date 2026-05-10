package com.taskflow.automate.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MeetingTaskExtractorTest {

    private MeetingTaskExtractor extractor;

    @Before
    public void setUp() {
        extractor = new MeetingTaskExtractor();
    }

    @Test
    public void englishIWill_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("I will send the report by Friday");
        assertNotNull(result);
        assertTrue(result.title.contains("send the report"));
        assertEquals(MeetingTaskExtractor.Language.ENGLISH, result.detectedLanguage);
        assertTrue(result.confidence >= 0.8f);
    }

    @Test
    public void englishNeedTo_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("We need to review the PR");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.ENGLISH, result.detectedLanguage);
    }

    @Test
    public void englishFollowUp_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("Let's follow up with the client");
        assertNotNull(result);
    }

    @Test
    public void englishActionItem_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("Action item: prepare slides for demo");
        assertNotNull(result);
        assertTrue(result.title.contains("prepare slides"));
    }

    @Test
    public void englishCasualSentence_returnsNull() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("The weather is nice today");
        assertNull(result);
    }

    @Test
    public void englishGreeting_returnsNull() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("Hello everyone, welcome to the meeting");
        assertNull(result);
    }

    @Test
    public void hindiKarnaHai_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("\u092E\u0941\u091D\u0947 \u0930\u093F\u092A\u094B\u0930\u094D\u091F \u092D\u0947\u091C\u0928\u093E \u0939\u0948");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.HINDI, result.detectedLanguage);
    }

    @Test
    public void hindiKaro_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("\u092F\u0947 \u0915\u093E\u092E \u0915\u0930\u094B");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.HINDI, result.detectedLanguage);
    }

    @Test
    public void hindiCasual_returnsNull() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("\u0906\u091C \u092E\u094C\u0938\u092E \u0905\u091A\u094D\u091B\u093E \u0939\u0948");
        assertNull(result);
    }

    @Test
    public void hinglishKarnaHai_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("mujhe report send karna hai");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.HINGLISH, result.detectedLanguage);
    }

    @Test
    public void hinglishSendKaro_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("email send karo client ko");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.HINGLISH, result.detectedLanguage);
    }

    @Test
    public void hinglishReviewKarLena_returnsActionable() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("code review kar lena please");
        assertNotNull(result);
        assertEquals(MeetingTaskExtractor.Language.HINGLISH, result.detectedLanguage);
    }

    @Test
    public void dateDetection_tomorrow_setsDueDate() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("I will do this tomorrow");
        assertNotNull(result);
        assertNotNull(result.dueDate);
        // Should be approximately 24 hours from now (within 25 hours to account for time-of-day)
        long now = System.currentTimeMillis();
        assertTrue(result.dueDate > now);
        assertTrue(result.dueDate - now < 48L * 60 * 60 * 1000);
    }

    @Test
    public void dateDetection_byMonday_setsDueDate() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("I will submit by Monday");
        assertNotNull(result);
        assertNotNull(result.dueDate);
        // Should be within 7 days from now
        long now = System.currentTimeMillis();
        assertTrue(result.dueDate > now);
        assertTrue(result.dueDate - now <= 7L * 24 * 60 * 60 * 1000);
    }

    @Test
    public void assigneeDetection_withTeamMember_setsAssignee() {
        extractor.setTeamMembers(Arrays.asList("Rahul", "Priya"));
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("Rahul will send the report");
        assertNotNull(result);
        assertEquals("Rahul", result.assigneeName);
    }

    @Test
    public void assigneeDetection_noMatch_nullAssignee() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("I will send the report");
        assertNotNull(result);
        assertNull(result.assigneeName);
    }

    @Test
    public void confidenceScoring_strongPattern_highConfidence() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("I will complete the task today");
        assertNotNull(result);
        assertTrue(result.confidence >= 0.8f);
    }

    @Test
    public void confidenceScoring_weakPattern_lowerConfidence() {
        MeetingTaskExtractor.ExtractedActionItem result =
                extractor.extractFromLine("We should consider updating the docs");
        assertNotNull(result);
        assertTrue(result.confidence <= 0.6f);
    }

    @Test
    public void nullInput_returnsNull() {
        MeetingTaskExtractor.ExtractedActionItem result = extractor.extractFromLine(null);
        assertNull(result);
    }

    @Test
    public void emptyInput_returnsNull() {
        MeetingTaskExtractor.ExtractedActionItem result = extractor.extractFromLine("");
        assertNull(result);
    }
}
