package com.taskflow.automate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.automate.model.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY priority ASC, created_at DESC")
    List<Task> getAllTasksByPriority();

    @Query("SELECT * FROM tasks WHERE status = 'pending'")
    List<Task> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(long id);

    @Query("UPDATE tasks SET status = 'completed' WHERE id = :id")
    void markComplete(long id);

    @Query("DELETE FROM tasks WHERE status = 'completed'")
    void deleteCompletedTasks();

    @Query("SELECT * FROM tasks WHERE notification_key = :key AND status = 'pending' LIMIT 1")
    Task getTaskByNotificationKey(String key);

    @Query("UPDATE tasks SET reminder_count = reminder_count + 1 WHERE id = :id")
    void incrementReminderCount(long id);
}
