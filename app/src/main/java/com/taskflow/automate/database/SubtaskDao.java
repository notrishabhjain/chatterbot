package com.taskflow.automate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.automate.model.Subtask;

import java.util.List;

@Dao
public interface SubtaskDao {

    @Insert
    long insertSubtask(Subtask subtask);

    @Update
    void updateSubtask(Subtask subtask);

    @Delete
    void deleteSubtask(Subtask subtask);

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY created_at ASC")
    List<Subtask> getSubtasksForTask(long taskId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE task_id = :taskId AND completed = 1")
    int getCompletedSubtaskCount(long taskId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE task_id = :taskId")
    int getTotalSubtaskCount(long taskId);
}
