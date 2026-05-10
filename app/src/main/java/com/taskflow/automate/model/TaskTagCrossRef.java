package com.taskflow.automate.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "task_tag_cross_ref", primaryKeys = {"taskId", "tagId"})
public class TaskTagCrossRef {

    @ColumnInfo(name = "task_id")
    private long taskId;

    @ColumnInfo(name = "tag_id")
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
