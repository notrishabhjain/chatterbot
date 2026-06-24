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
        // Calendar app score 2 + keyword "meeting" from title = score 1 = total 3 = High
        int priority = priorityAssigner.assignPriority(
                "com.google.android.calendar", "Meeting", "Team standup at 10am", null);
        assertEquals(1, priority); // High
    }

    @Test
    public void gmailApp_getsMediumPriority() {
        // Gmail app score 2, no boost keywords = total 2 >= 1 = Medium
        int priority = priorityAssigner.assignPriority(
                "com.google.android.gm", "New Email", "Project status", null);
        assertEquals(2, priority); // Medium
    }

    @Test
    public void calendarApp_withKeyword_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.google.android.calendar", "Urgent Meeting", "deadline approaching", null);
        // App score 2 + keyword score (urgent + deadline = 2 keywords = score 2) = 4 >= 3 = High
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
        // App score 0 + keyword score (urgent = 1 keyword = score 1) = 1 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void deadlineKeyword_boostsPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Project", "The deadline is approaching", null);
        // App score 0 + keyword score (deadline = 1 keyword = score 1) = 1 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void multipleBoostKeywords_getsHighPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Urgent", "This is urgent, deadline is critical", null);
        // App score 0 + keyword score (urgent + deadline + critical = 3 keywords = score 3) = 3 >= 3 = High
        assertEquals(1, priority);
    }

    @Test
    public void dueWithinOneHour_boostsToHigh() {
        long dueDate = System.currentTimeMillis() + 30 * 60 * 1000L; // 30 minutes from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Task", "Complete this", dueDate);
        // App score 0 + keyword score 0 + time score 3 = 3 >= 3 = High
        assertEquals(1, priority);
    }

    @Test
    public void dueWithin24Hours_getsMediumPriority() {
        long dueDate = System.currentTimeMillis() + 12 * 60 * 60 * 1000L; // 12 hours from now
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Task", "Complete this", dueDate);
        // App score 0 + keyword score 0 + time score 1 = 1 >= 1 = Medium
        assertEquals(2, priority);
    }

    @Test
    public void noKeywords_noSpecialApp_noDueDate_getsLowPriority() {
        int priority = priorityAssigner.assignPriority(
                "com.some.random.app", "Hello", "Just a message", null);
        // App score 0 + keyword score 0 + time score 0 = 0 = Low
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
        // App score 0 + keyword score (due = 1 keyword = score 1) + time score 3 = 4 >= 3 = High
        assertEquals(1, priority);
    }
}
