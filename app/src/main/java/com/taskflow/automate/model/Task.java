package com.taskflow.automate.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "source_app")
    private String sourceApp;

    @ColumnInfo(name = "priority")
    private int priority; // 1 = High, 2 = Medium, 3 = Low

    @ColumnInfo(name = "due_date")
    private Long dueDate; // nullable, epoch millis

    @ColumnInfo(name = "created_at")
    private long createdAt; // epoch millis

    @ColumnInfo(name = "status")
    private String status; // "pending" or "completed"

    @ColumnInfo(name = "notification_key")
    private String notificationKey;

    @ColumnInfo(name = "reminder_count", defaultValue = "0")
    private int reminderCount;

    @ColumnInfo(name = "completed_at")
    private Long completedAt;

    @ColumnInfo(name = "assignee")
    private String assignee;

    @ColumnInfo(name = "is_follow_up", defaultValue = "0")
    private boolean isFollowUp;

    @ColumnInfo(name = "linked_task_id")
    private Long linkedTaskId;

    @ColumnInfo(name = "recurrence_rule")
    private String recurrenceRule;

    @ColumnInfo(name = "recurrence_interval", defaultValue = "0")
    private int recurrenceInterval;

    public Task() {
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceApp() {
        return sourceApp;
    }

    public int getPriority() {
        return priority;
    }

    public Long getDueDate() {
        return dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public String getAssignee() {
        return assignee;
    }

    public boolean isFollowUp() {
        return isFollowUp;
    }

    public Long getLinkedTaskId() {
        return linkedTaskId;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    public int getRecurrenceInterval() {
        return recurrenceInterval;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSourceApp(String sourceApp) {
        this.sourceApp = sourceApp;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNotificationKey(String notificationKey) {
        this.notificationKey = notificationKey;
    }

    public void setReminderCount(int reminderCount) {
        this.reminderCount = reminderCount;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setFollowUp(boolean followUp) {
        isFollowUp = followUp;
    }

    public void setLinkedTaskId(Long linkedTaskId) {
        this.linkedTaskId = linkedTaskId;
    }

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public void setRecurrenceInterval(int recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    // Helper method
    public String getPriorityLabel() {
        switch (priority) {
            case 1:
                return "High";
            case 2:
                return "Medium";
            case 3:
                return "Low";
            default:
                return "Low";
        }
    }
}
