package com.taskflow.automate.util;

import com.taskflow.automate.model.Task;

import java.util.Calendar;

public class RecurringTaskManager {

    /**
     * Creates a new pending task based on a completed recurring task.
     * Returns the new Task (not yet inserted) or null if task is not recurring.
     */
    public Task createNextRecurrence(Task completedTask) {
        if (completedTask.getRecurrenceRule() == null) return null;

        Task newTask = new Task();
        newTask.setTitle(completedTask.getTitle());
        newTask.setDescription(completedTask.getDescription());
        newTask.setPriority(completedTask.getPriority());
        newTask.setAssignee(completedTask.getAssignee());
        newTask.setRecurrenceRule(completedTask.getRecurrenceRule());
        newTask.setRecurrenceInterval(completedTask.getRecurrenceInterval());
        newTask.setSourceApp(completedTask.getSourceApp());
        newTask.setStatus("pending");
        newTask.setCreatedAt(System.currentTimeMillis());

        // Calculate next due date
        Long baseDueDate = completedTask.getDueDate();
        if (baseDueDate == null) baseDueDate = System.currentTimeMillis();

        long nextDue;
        int interval = completedTask.getRecurrenceInterval() > 0 ? completedTask.getRecurrenceInterval() : 1;
        switch (completedTask.getRecurrenceRule()) {
            case "DAILY":
                nextDue = baseDueDate + (interval * 24L * 60 * 60 * 1000);
                break;
            case "WEEKLY":
                nextDue = baseDueDate + (interval * 7L * 24 * 60 * 60 * 1000);
                break;
            case "MONTHLY":
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(baseDueDate);
                cal.add(Calendar.MONTH, interval);
                nextDue = cal.getTimeInMillis();
                break;
            default:
                return null;
        }
        newTask.setDueDate(nextDue);
        return newTask;
    }
}
