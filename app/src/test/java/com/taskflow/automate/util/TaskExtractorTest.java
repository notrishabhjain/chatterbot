package com.taskflow.automate.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskExtractorTest {

    private TaskExtractor taskExtractor;

    @Before
    public void setUp() {
        taskExtractor = new TaskExtractor();
    }

    // --- Non-actionable package filtering ---

    @Test
    public void nonActionablePackage_spotify_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Now Playing", "Some song", null, null, "com.spotify.music");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    @Test
    public void nonActionablePackage_systemUi_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("System Update", "Update available", null, null, "com.android.systemui");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    @Test
    public void nonActionablePackage_android_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("System", "Process running", null, null, "android");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    // --- Non-actionable content filtering ---

    @Test
    public void nonActionableContent_music_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Now Playing", "Artist - Song", null, null, "com.some.app");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    @Test
    public void nonActionableContent_charging_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Charging", "Battery at 50%", null, null, "com.some.app");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    @Test
    public void nonActionableContent_download_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Download complete", "file.zip", null, null, "com.some.app");
        assertFalse(result.isActionable);
        assertEquals(0, result.actionabilityScore);
    }

    // --- Scoring system: actionable notifications ---

    @Test
    public void highActionKeyword_urgent_scoresAboveThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "This is urgent, handle it now", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
    }

    @Test
    public void highActionKeyword_asap_scoresAboveThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Need this done asap please", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
    }

    @Test
    public void mediumActionKeyword_pleaseReview_scoresAboveThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("John", "Please review the document by tomorrow", null, null, "com.whatsapp");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
        assertEquals("John", result.taskTitle);
        assertEquals("Please review the document by tomorrow", result.taskDescription);
    }

    @Test
    public void calendarPackage_scoresAboveThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team Standup", "In 10 minutes", null, null, "com.google.android.calendar");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
        assertEquals("Team Standup", result.taskTitle);
    }

    @Test
    public void emailPackage_scoresAboveThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("New Email", "Project update from client", null, null, "com.microsoft.office.outlook");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
    }

    // --- Scoring system: non-actionable casual messages ---

    @Test
    public void casualMessage_hey_scoresBelowThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Friend", "Hey how are you", null, null, "com.some.random.app");
        assertFalse(result.isActionable);
        assertTrue(result.actionabilityScore < 25);
    }

    @Test
    public void casualMessage_lol_scoresBelowThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Group Chat", "lol", null, null, "com.some.random.app");
        assertFalse(result.isActionable);
        assertTrue(result.actionabilityScore < 25);
    }

    @Test
    public void casualMessage_ok_scoresBelowThreshold() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Friend", "ok", null, null, "com.some.random.app");
        assertFalse(result.isActionable);
        assertTrue(result.actionabilityScore < 25);
    }

    // --- Assigner extraction ---

    @Test
    public void assignerExtraction_fromTitle() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Sarah Johnson", "Please submit the report by tomorrow", null, null, "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("Sarah Johnson", result.assigner);
    }

    @Test
    public void assignerExtraction_genericTitle_returnsNull() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Notification", "Please complete this task urgently", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNull(result.assigner);
    }

    // --- Task type classification ---

    @Test
    public void taskTypeClassification_meeting() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Calendar", "Team meeting at 3pm today", null, null, "com.google.android.calendar");
        assertTrue(result.isActionable);
        assertEquals("MEETING", result.taskType);
    }

    @Test
    public void taskTypeClassification_deadline() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "The deadline for this report is tomorrow", null, null, "com.slack");
        assertTrue(result.isActionable);
        assertEquals("DEADLINE", result.taskType);
    }

    @Test
    public void taskTypeClassification_followUp() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Colleague", "Just following up on the proposal, any update?", null, null, "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("FOLLOW_UP", result.taskType);
    }

    @Test
    public void taskTypeClassification_request() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team Lead", "Can you send me the latest figures by EOD?", null, null, "com.slack");
        assertTrue(result.isActionable);
        assertEquals("REQUEST", result.taskType);
    }

    @Test
    public void taskTypeClassification_approval() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("HR System", "Please approve the leave request", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertEquals("APPROVAL", result.taskType);
    }

    @Test
    public void taskTypeClassification_reminder() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("App", "Don't forget to submit your timesheet this week", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertEquals("REMINDER", result.taskType);
    }

    @Test
    public void taskTypeClassification_general() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Calendar", "Event starting soon in the conference room", null, null, "com.google.android.calendar");
        assertTrue(result.isActionable);
        assertEquals("GENERAL", result.taskType);
    }

    // --- isFollowUp detection ---

    @Test
    public void followUpDetection_followingUp_true() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Following up on my earlier request, please respond", null, null, "com.slack");
        assertTrue(result.isActionable);
        assertTrue(result.isFollowUp);
    }

    @Test
    public void followUpDetection_noFollowUpKeyword_false() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "Please review the document urgently", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertFalse(result.isFollowUp);
    }

    // --- Date extraction ---

    @Test
    public void dueDateExtraction_tomorrow_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Task", "Please submit this tomorrow", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long expectedMin = System.currentTimeMillis() + 23 * 60 * 60 * 1000L;
        long expectedMax = System.currentTimeMillis() + 25 * 60 * 60 * 1000L;
        assertTrue(result.dueDateHint > expectedMin && result.dueDateHint < expectedMax);
    }

    @Test
    public void dueDateExtraction_today_parsesToEndOfDay() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Need this today please", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long now = System.currentTimeMillis();
        assertTrue(result.dueDateHint >= now);
        assertTrue(result.dueDateHint - now < 24 * 60 * 60 * 1000L);
    }

    @Test
    public void dueDateExtraction_inHours_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Alert", "Please reply in 2 hours", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long expected = System.currentTimeMillis() + 2 * 60 * 60 * 1000L;
        assertTrue(Math.abs(result.dueDateHint - expected) < 5000);
    }

    @Test
    public void dueDateExtraction_byTime_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Deadline", "Please submit by 5pm", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        assertTrue(result.dueDateHint > System.currentTimeMillis() - 1000);
    }

    @Test
    public void dueDateExtraction_nextMonday_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "Please have this ready by next Monday", null, null, "com.slack");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        // Should be in the future
        assertTrue(result.dueDateHint > System.currentTimeMillis());
        // Should be within 8 days
        assertTrue(result.dueDateHint - System.currentTimeMillis() <= 8 * 24 * 60 * 60 * 1000L);
    }

    @Test
    public void dueDateExtraction_endOfWeek_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Lead", "Need this done by end of week please", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        assertTrue(result.dueDateHint > System.currentTimeMillis());
    }

    @Test
    public void dueDateExtraction_thisFriday_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Submit the report this friday please", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        assertTrue(result.dueDateHint > System.currentTimeMillis());
    }

    @Test
    public void dueDateExtraction_noTimeReference_returnsNull() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "Please review this document", null, null, "com.some.app");
        assertTrue(result.isActionable);
        assertNull(result.dueDateHint);
    }

    // --- bigText / subText handling ---

    @Test
    public void bigText_contributesToScoring() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Email", "New message",
                        "Please review and approve the budget proposal by tomorrow", null, "com.some.app");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
    }

    @Test
    public void subText_contributesToScoring() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Notification", "New item",
                        null, "Urgent: deadline approaching, action required", "com.some.app");
        assertTrue(result.isActionable);
        assertTrue(result.actionabilityScore >= 25);
    }

    @Test
    public void bigTextAndSubText_combinedForAnalysis() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team", "Update",
                        "We have a meeting scheduled", "Please confirm attendance", "com.some.app");
        assertTrue(result.isActionable);
        assertEquals("MEETING", result.taskType);
    }

    // --- Source notification text stored ---

    @Test
    public void sourceNotificationText_storesCombinedText() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Please review the PR urgently", null, null, "com.slack");
        assertTrue(result.isActionable);
        assertNotNull(result.sourceNotificationText);
        assertTrue(result.sourceNotificationText.contains("Boss"));
        assertTrue(result.sourceNotificationText.contains("Please review the PR urgently"));
    }

    // --- Legacy API (backward compatibility) ---

    @Test
    public void legacyApi_threeParams_stillWorks() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("John", "Please review the document", "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("John", result.assigner);
    }

    @Test
    public void legacyApi_nonActionable_stillFiltered() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Now Playing", "Some song", "com.spotify.music");
        assertFalse(result.isActionable);
    }
}
