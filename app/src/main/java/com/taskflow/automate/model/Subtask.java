package com.taskflow.automate.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "subtasks",
        foreignKeys = @ForeignKey(
            entity = Task.class,
            parentColumns = "id",
            childColumns = "task_id",
            onDelete = ForeignKey.CASCADE
        ))
public class Subtask {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "task_id")
    private long taskId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "completed", defaultValue = "0")
    private boolean completed;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public Subtask() {
    }

    // Getters
    public long getId() {
        return id;
    }

    public long getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
