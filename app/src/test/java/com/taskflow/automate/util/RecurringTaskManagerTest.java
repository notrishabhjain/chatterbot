package com.taskflow.automate.util;

import com.taskflow.automate.model.Task;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecurringTaskManagerTest {

    private RecurringTaskManager manager;

    @Before
    public void setUp() {
        manager = new RecurringTaskManager();
    }

    @Test
    public void dailyRecurrence_addsOneDay() {
        Task task = new Task();
        task.setTitle("Daily standup");
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(1);
        long baseDue = System.currentTimeMillis();
        task.setDueDate(baseDue);

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        long expectedDue = baseDue + 24L * 60 * 60 * 1000;
        assertEquals(expectedDue, result.getDueDate().longValue());
    }

    @Test
    public void weeklyRecurrence_addsSevenDays() {
        Task task = new Task();
        task.setTitle("Weekly review");
        task.setRecurrenceRule("WEEKLY");
        task.setRecurrenceInterval(1);
        long baseDue = System.currentTimeMillis();
        task.setDueDate(baseDue);

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        long expectedDue = baseDue + 7L * 24 * 60 * 60 * 1000;
        assertEquals(expectedDue, result.getDueDate().longValue());
    }

    @Test
    public void monthlyRecurrence_addsOneMonth() {
        Task task = new Task();
        task.setTitle("Monthly report");
        task.setRecurrenceRule("MONTHLY");
        task.setRecurrenceInterval(1);
        long baseDue = System.currentTimeMillis();
        task.setDueDate(baseDue);

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        // Monthly adds approximately 28-31 days
        long diff = result.getDueDate() - baseDue;
        assertTrue(diff >= 27L * 24 * 60 * 60 * 1000);
        assertTrue(diff <= 32L * 24 * 60 * 60 * 1000);
    }

    @Test
    public void noRecurrenceRule_returnsNull() {
        Task task = new Task();
        task.setTitle("One-off task");
        task.setRecurrenceRule(null);

        Task result = manager.createNextRecurrence(task);
        assertNull(result);
    }

    @Test
    public void newTaskHasPendingStatus() {
        Task task = new Task();
        task.setTitle("Recurring task");
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());
        task.setStatus("completed");

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        assertEquals("pending", result.getStatus());
    }

    @Test
    public void newTaskCopiesTitle() {
        Task task = new Task();
        task.setTitle("Important recurring task");
        task.setRecurrenceRule("WEEKLY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        assertEquals("Important recurring task", result.getTitle());
    }

    @Test
    public void newTaskCopiesPriority() {
        Task task = new Task();
        task.setTitle("High priority task");
        task.setPriority(1);
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        assertEquals(1, result.getPriority());
    }

    @Test
    public void newTaskCopiesAssignee() {
        Task task = new Task();
        task.setTitle("Assigned task");
        task.setAssignee("Priya");
        task.setRecurrenceRule("WEEKLY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        assertEquals("Priya", result.getAssignee());
    }

    @Test
    public void newTaskHasFreshCreatedAt() {
        Task task = new Task();
        task.setTitle("Old task");
        task.setCreatedAt(1000L);
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());

        long beforeCreate = System.currentTimeMillis();
        Task result = manager.createNextRecurrence(task);
        long afterCreate = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getCreatedAt() >= beforeCreate);
        assertTrue(result.getCreatedAt() <= afterCreate);
    }

    @Test
    public void customInterval_daily_addsMultipleDays() {
        Task task = new Task();
        task.setTitle("Every 3 days task");
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(3);
        long baseDue = System.currentTimeMillis();
        task.setDueDate(baseDue);

        Task result = manager.createNextRecurrence(task);
        assertNotNull(result);
        long expectedDue = baseDue + 3L * 24 * 60 * 60 * 1000;
        assertEquals(expectedDue, result.getDueDate().longValue());
    }

    @Test
    public void nullDueDate_usesCurrentTime() {
        Task task = new Task();
        task.setTitle("No due date task");
        task.setRecurrenceRule("DAILY");
        task.setRecurrenceInterval(1);
        task.setDueDate(null);

        long beforeCreate = System.currentTimeMillis();
        Task result = manager.createNextRecurrence(task);
        long afterCreate = System.currentTimeMillis();

        assertNotNull(result);
        // New due date should be approximately current time + 1 day
        long oneDayMs = 24L * 60 * 60 * 1000;
        assertTrue(result.getDueDate() >= beforeCreate + oneDayMs);
        assertTrue(result.getDueDate() <= afterCreate + oneDayMs);
    }

    @Test
    public void unknownRecurrenceRule_returnsNull() {
        Task task = new Task();
        task.setTitle("Unknown rule task");
        task.setRecurrenceRule("YEARLY");
        task.setRecurrenceInterval(1);
        task.setDueDate(System.currentTimeMillis());

        Task result = manager.createNextRecurrence(task);
        assertNull(result);
    }
}
