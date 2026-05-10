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

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date ASC, created_at DESC")
    List<Task> getAllTasksByDeadline();

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND assigner = :assigner ORDER BY priority ASC")
    List<Task> getTasksByAssigner(String assigner);

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND is_follow_up = 1 ORDER BY priority ASC, created_at DESC")
    List<Task> getFollowUpTasks();

    @Query("SELECT DISTINCT assigner FROM tasks WHERE status = 'pending' AND assigner IS NOT NULL")
    List<String> getAllAssigners();

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND due_date < :currentTime ORDER BY due_date ASC")
    List<Task> getOverdueTasks(long currentTime);
}
