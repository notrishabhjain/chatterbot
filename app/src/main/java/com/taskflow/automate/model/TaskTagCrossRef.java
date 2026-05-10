package com.taskflow.automate.model;

import androidx.room.Entity;

@Entity(tableName = "task_tag_cross_ref", primaryKeys = {"taskId", "tagId"})
public class TaskTagCrossRef {

    private long taskId;

    private long tagId;

    public TaskTagCrossRef() {
    }

    // Getters
    public long getTaskId() {
        return taskId;
    }

    public long getTagId() {
        return tagId;
    }

    // Setters
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}
