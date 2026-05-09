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

    @Test
    public void nonActionablePackage_spotify_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Now Playing", "Some song", "com.spotify.music");
        assertFalse(result.isActionable);
    }

    @Test
    public void nonActionablePackage_systemUi_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("System Update", "Update available", "com.android.systemui");
        assertFalse(result.isActionable);
    }

    @Test
    public void nonActionablePackage_android_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("System", "Process running", "android");
        assertFalse(result.isActionable);
    }

    @Test
    public void nonActionableContent_music_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Now Playing", "Artist - Song", "com.some.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void nonActionableContent_charging_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Charging", "Battery at 50%", "com.some.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void nonActionableContent_download_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Download complete", "file.zip", "com.some.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void calendarPackage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team Standup", "In 10 minutes", "com.google.android.calendar");
        assertTrue(result.isActionable);
        assertEquals("Team Standup", result.taskTitle);
    }

    @Test
    public void emailPackage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("New Email", "Project update", "com.microsoft.office.outlook");
        assertTrue(result.isActionable);
    }

    @Test
    public void outlookPackage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Meeting Invite", "Sprint review", "com.microsoft.office.outlook");
        assertTrue(result.isActionable);
    }

    @Test
    public void messageWithActionKeyword_pleaseReview_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("John", "Please review the document", "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("John", result.taskTitle);
        assertEquals("Please review the document", result.taskDescription);
    }

    @Test
    public void messageWithActionKeyword_need_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "I need this done by EOD", "com.slack");
        assertTrue(result.isActionable);
    }

    @Test
    public void messageWithoutActionKeywords_fromNonPriorityApp_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Friend", "Hey how are you?", "com.some.random.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void messageWithoutActionKeywords_casualChat_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Group Chat", "Lol that was funny", "com.whatsapp");
        assertFalse(result.isActionable);
    }

    @Test
    public void dueDateExtraction_tomorrow_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Task", "Please submit this tomorrow", "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long expectedMin = System.currentTimeMillis() + 23 * 60 * 60 * 1000L;
        long expectedMax = System.currentTimeMillis() + 25 * 60 * 60 * 1000L;
        assertTrue(result.dueDateHint > expectedMin && result.dueDateHint < expectedMax);
    }

    @Test
    public void dueDateExtraction_today_parsesToEndOfDay() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "Need this today please", "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        // Should be end of today (23:59:59)
        long now = System.currentTimeMillis();
        assertTrue(result.dueDateHint >= now);
        // Should be within 24 hours
        assertTrue(result.dueDateHint - now < 24 * 60 * 60 * 1000L);
    }

    @Test
    public void dueDateExtraction_inHours_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Alert", "Please reply in 2 hours", "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long expected = System.currentTimeMillis() + 2 * 60 * 60 * 1000L;
        // Allow 5 seconds tolerance for test execution time
        assertTrue(Math.abs(result.dueDateHint - expected) < 5000);
    }

    @Test
    public void dueDateExtraction_byTime_parsesCorrectly() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Deadline", "Please submit by 5pm", "com.some.app");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        // Should be a valid timestamp in the future
        assertTrue(result.dueDateHint > System.currentTimeMillis() - 1000);
    }

    @Test
    public void dueDateExtraction_noTimeReference_returnsNull() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "Please review this", "com.some.app");
        assertTrue(result.isActionable);
        assertNull(result.dueDateHint);
    }
}
