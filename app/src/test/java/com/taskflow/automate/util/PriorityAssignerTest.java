package com.taskflow.automate.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PriorityAssignerTest {

    private PriorityAssigner priorityAssigner;

    @Before
    public void setUp() {
        priorityAssigner = new PriorityAssigner();
    }

    @Test
    public void calendarApp_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.google.android.calendar", "Meeting", "Team standup at 10am", null);
        assertEquals(1, priority); // High
    }

    @Test
    public void gmailApp_getsMediumPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.google.android.gm", "New Email", "Project status", null);
        assertEquals(2, priority); // Medium
    }

    @Test
    public void calendarApp_withKeyword_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.google.android.calendar", "Urgent Meeting", "deadline approaching", null);
        assertEquals(1, priority);
    }

    @Test
    public void socialMediaApp_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.instagram.android", "New post", "Someone liked your photo", null);
        assertEquals(3, priority); // Low
    }

    @Test
    public void twitterApp_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.twitter.android", "New mention", "Someone mentioned you", null);
        assertEquals(3, priority); // Low
    }

    @Test
    public void facebookApp_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.facebook.katana", "Notification", "Friend posted a photo", null);
        assertEquals(3, priority); // Low
    }

    @Test
    public void urgentKeyword_boostsPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Alert", "This is urgent please respond", null);
        assertEquals(2, priority);
    }

    @Test
    public void deadlineKeyword_boostsPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Project", "The deadline is approaching", null);
        assertEquals(2, priority);
    }

    @Test
    public void multipleBoostKeywords_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Urgent", "This is urgent, deadline is critical", null);
        assertEquals(1, priority);
    }

    @Test
    public void dueWithinOneHour_boostsToHigh() {
        long dueDate = System.currentTimeMillis() + 30 * 60 * 1000L; // 30 minutes from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Task", "Complete this", dueDate);
        assertEquals(1, priority);
    }

    @Test
    public void dueWithin24Hours_getsMediumPriority() {
        long dueDate = System.currentTimeMillis() + 12 * 60 * 60 * 1000L; // 12 hours from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Task", "Complete this", dueDate);
        assertEquals(2, priority);
    }

    @Test
    public void noKeywords_noSpecialApp_noDueDate_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Hello", "Just a message", null);
        assertEquals(3, priority);
    }

    @Test
    public void nullPackageName_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                null, "Hello", "Just a message", null);
        assertEquals(3, priority);
    }

    @Test
    public void overdueTask_getsHighPriority() {
        long dueDate = System.currentTimeMillis() - 60 * 60 * 1000L; // 1 hour ago
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Task", "Was due earlier", dueDate);
        assertEquals(1, priority);
    }

    // --- Task type scoring tests ---

    @Test
    public void deadlineTaskType_boostsPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Report", "Submit the report", null, "DEADLINE");
        // App score 0 + keyword score 0 + time score 0 + taskType score 1 = 1 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void approvalTaskType_boostsPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Request", "New approval needed", null, "APPROVAL");
        // App score 0 + keyword score 0 + time score 0 + taskType score 1 = 1 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void meetingTaskType_withinTwoHours_boostsPriority() {
        long dueDate = System.currentTimeMillis() + 90 * 60 * 1000L; // 90 minutes from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Standup", "Daily standup", dueDate, "MEETING");
        // App score 0 + keyword score 0 + time score 1 (within 24h) + taskType score 1 (meeting within 2h) = 2 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void meetingTaskType_notWithinTwoHours_noBoost() {
        long dueDate = System.currentTimeMillis() + 5 * 60 * 60 * 1000L; // 5 hours from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Standup", "Daily standup", dueDate, "MEETING");
        // App score 0 + keyword score (meeting=1) + time score 1 (within 24h) + taskType score 0 = 2 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void generalTaskType_noBoost() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Hello", "Just a message", null, "GENERAL");
        // App score 0 + keyword score 0 + time score 0 + taskType score 0 = 0 = Low
        assertEquals(3, priority);
    }

    @Test
    public void nullTaskType_noBoost() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Hello", "Just a message", null, null);
        assertEquals(3, priority);
    }

    @Test
    public void deadlineType_withUrgentKeyword_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Urgent", "Complete this task", null, "DEADLINE");
        // App score 0 + keyword score 1 (urgent) + time score 0 + taskType score 1 = 2 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void deadlineType_withHighPriorityApp_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.google.android.gm", "Report", "Submit the report", null, "DEADLINE");
        // App score 2 + keyword score 0 + time score 0 + taskType score 1 = 3 >= 3 = High
        assertEquals(1, priority);
    }
}
