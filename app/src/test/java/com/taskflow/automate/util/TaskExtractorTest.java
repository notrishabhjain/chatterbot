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

    // --- Non-actionable package tests ---

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

    // --- Non-actionable content tests ---

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

    // --- Always-actionable package tests ---

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
    public void whatsApp_anyMessage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("John", "Can you check this?", "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("John", result.taskTitle);
        assertEquals("Can you check this?", result.taskDescription);
    }

    @Test
    public void whatsApp_casualChat_returnsActionable() {
        // WhatsApp is in ALWAYS_ACTIONABLE_PACKAGES, so even casual messages are captured
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Group Chat", "Lol that was funny", "com.whatsapp");
        assertTrue(result.isActionable);
    }

    @Test
    public void whatsApp_hindiMessage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Rahul", "Yeh file bhej do kal tak", "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("Rahul", result.taskTitle);
    }

    @Test
    public void telegram_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Work Group", "Meeting shifted to 3pm", "org.telegram.messenger");
        assertTrue(result.isActionable);
    }

    @Test
    public void slack_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("#general", "Deployment at 5pm today", "com.slack");
        assertTrue(result.isActionable);
    }

    @Test
    public void teams_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "Sprint planning tomorrow", "com.microsoft.teams");
        assertTrue(result.isActionable);
    }

    // --- Play Store no longer blocked ---

    @Test
    public void playStore_noLongerBlocked_returnsActionable() {
        // com.android.vending was removed from NON_ACTIONABLE_PACKAGES
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("App Update", "Your app has been updated successfully", "com.android.vending");
        assertTrue(result.isActionable);
    }

    @Test
    public void playStore_shortNotification_returnsNotActionable() {
        // Play Store with very short content should still be rejected
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Hi", "", "com.android.vending");
        assertFalse(result.isActionable);
    }

    // --- Action keyword tests ---

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
    public void messageWithActionKeyword_canYou_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "can you finish the report?", "com.some.random.app");
        assertTrue(result.isActionable);
    }

    // --- Hindi/Hinglish keyword tests ---

    @Test
    public void hindiKeyword_karo_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team", "karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void hindiKeyword_jaldi_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("PM", "jaldi bhejo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void hindiKeyword_zaruri_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Lead", "zaruri hai ye", "com.some.app");
        assertTrue(result.isActionable);
    }

    // --- Short notification rejection ---

    @Test
    public void shortNotification_singleChar_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("", "a", "com.some.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void shortNotification_twoChars_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("", "ab", "com.some.app");
        assertFalse(result.isActionable);
    }

    @Test
    public void shortNotification_emptyBoth_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("", "", "com.some.app");
        assertFalse(result.isActionable);
    }

    // --- Group message summary rejection ---

    @Test
    public void groupMessageSummary_noContent_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("WhatsApp", "5 new messages", "com.whatsapp");
        assertFalse(result.isActionable);
    }

    @Test
    public void groupMessageSummary_singleMessage_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Group", "1 message", "com.whatsapp");
        assertFalse(result.isActionable);
    }

    @Test
    public void groupMessageWithContent_returnsActionable() {
        // Has a colon indicating actual message content, not just summary
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Work Group", "John: 2 messages need your review", "com.whatsapp");
        assertTrue(result.isActionable);
    }

    // --- Meaningful content from unknown apps ---

    @Test
    public void unknownApp_meaningfulContent_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Friend", "Hey how are you doing today?", "com.some.random.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void unknownApp_shortContent_noKeyword_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("App", "Hi there", "com.some.random.app");
        assertFalse(result.isActionable);
    }

    // --- Due date extraction tests ---

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

    @Test
    public void dueDateExtraction_hindiKalTak_parsesTomorrow() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Rahul", "File bhej do kal tak", "com.whatsapp");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long expectedMin = System.currentTimeMillis() + 23 * 60 * 60 * 1000L;
        long expectedMax = System.currentTimeMillis() + 25 * 60 * 60 * 1000L;
        assertTrue(result.dueDateHint > expectedMin && result.dueDateHint < expectedMax);
    }

    @Test
    public void dueDateExtraction_hindiAaj_parsesToday() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team", "aaj submit karo", "com.whatsapp");
        assertTrue(result.isActionable);
        assertNotNull(result.dueDateHint);
        long now = System.currentTimeMillis();
        assertTrue(result.dueDateHint >= now);
        assertTrue(result.dueDateHint - now < 24 * 60 * 60 * 1000L);
    }

    // --- New keyword tests for expanded ACTION_KEYWORDS ---

    @Test
    public void newEnglishKeyword_delegate_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Manager", "delegate this", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newEnglishKeyword_brainstorm_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Team", "brainstorm", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newEnglishKeyword_troubleshoot_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Dev", "troubleshoot", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newEnglishKeyword_expedite_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("PM", "expedite", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newEnglishKeyword_authorize_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Admin", "authorize", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHindiKeyword_samjho_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Teacher", "samjho", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHindiKeyword_shuruKaro_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Lead", "shuru karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHindiKeyword_khatamKaro_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("PM", "khatam karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHinglishPhrase_meetingScheduleKaro_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Boss", "meeting schedule karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHinglishPhrase_codeReviewKaro_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Dev", "code review karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHinglishPhrase_deployKaro_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("DevOps", "deploy karo", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHindiUnicode_suno_returnsActionable() {
        // सुनो
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Friend", "\u0938\u0941\u0928\u094B", "com.some.app");
        assertTrue(result.isActionable);
    }

    @Test
    public void newHindiUnicode_shuru_karo_returnsActionable() {
        // शुरू करो
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Lead", "\u0936\u0941\u0930\u0942 \u0915\u0930\u094B", "com.some.app");
        assertTrue(result.isActionable);
    }

    // --- WhatsApp edge case tests ---

    @Test
    public void whatsApp_nullTitle_withMessage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(null, "Hey, can you send the report?", "com.whatsapp");
        assertTrue(result.isActionable);
        assertEquals("Notification Task", result.taskTitle);
        assertEquals("Hey, can you send the report?", result.taskDescription);
    }

    @Test
    public void whatsApp_nullTitle_emptyText_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(null, "", "com.whatsapp");
        assertFalse(result.isActionable);
    }

    @Test
    public void whatsApp_nullTitle_nullText_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(null, null, "com.whatsapp");
        assertFalse(result.isActionable);
    }

    @Test
    public void whatsAppBusiness_anyMessage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask("Business Contact", "Your order is ready for pickup", "com.whatsapp.w4b");
        assertTrue(result.isActionable);
        assertEquals("Business Contact", result.taskTitle);
        assertEquals("Your order is ready for pickup", result.taskDescription);
    }

    @Test
    public void whatsAppBusiness_nullTitle_withMessage_returnsActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(null, "Please confirm your appointment", "com.whatsapp.w4b");
        assertTrue(result.isActionable);
        assertEquals("Notification Task", result.taskTitle);
        assertEquals("Please confirm your appointment", result.taskDescription);
    }

    @Test
    public void whatsAppBusiness_nullTitle_emptyText_returnsNotActionable() {
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(null, "", "com.whatsapp.w4b");
        assertFalse(result.isActionable);
    }
}
