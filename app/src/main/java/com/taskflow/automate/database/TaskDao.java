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

    @Query("SELECT * FROM tasks WHERE status = 'completed' ORDER BY completed_at DESC")
    List<Task> getCompletedTasks();

    @Query("UPDATE tasks SET status = 'pending', completed_at = null WHERE id = :id")
    void restoreTask(long id);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteTask(long id);

    @Query("SELECT * FROM tasks WHERE due_date BETWEEN :startOfDay AND :endOfDay AND status = 'pending' ORDER BY priority ASC")
    List<Task> getTasksDueToday(long startOfDay, long endOfDay);

    @Query("SELECT * FROM tasks WHERE due_date < :now AND due_date IS NOT NULL AND status = 'pending' ORDER BY due_date ASC")
    List<Task> getOverdueTasks(long now);

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND status = 'pending' ORDER BY priority ASC")
    List<Task> searchTasks(String query);

    @Query("SELECT * FROM tasks WHERE priority = :priority AND status = 'pending' ORDER BY created_at DESC")
    List<Task> getTasksByPriority(int priority);

    @Query("SELECT * FROM tasks WHERE assignee = :assignee AND status = 'pending' ORDER BY priority ASC")
    List<Task> getTasksByAssignee(String assignee);

    @Query("SELECT * FROM tasks WHERE source_app = :sourceApp AND status = 'pending' ORDER BY created_at DESC")
    List<Task> getTasksBySourceApp(String sourceApp);

    @Query("UPDATE tasks SET status = 'completed', completed_at = :completedAt WHERE id = :id")
    void markCompleteWithTimestamp(long id, long completedAt);

    @Query("SELECT * FROM tasks WHERE recurrence_rule IS NOT NULL AND status = 'completed' ORDER BY completed_at DESC")
    List<Task> getCompletedRecurringTasks();

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    int getTaskCountByStatus(String status);

    @Query("SELECT COUNT(*) FROM tasks WHERE priority = 1 AND status = 'pending'")
    int getHighPriorityPendingCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE due_date BETWEEN :startOfDay AND :endOfDay AND status = 'pending'")
    int getDueTodayCount(long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM tasks WHERE due_date < :now AND due_date IS NOT NULL AND status = 'pending'")
    int getOverdueCount(long now);

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'completed' AND completed_at BETWEEN :weekStart AND :weekEnd")
    int getCompletedThisWeekCount(long weekStart, long weekEnd);

    @Query("SELECT * FROM tasks WHERE starred = 1 AND status = 'pending' ORDER BY priority ASC, created_at DESC")
    List<Task> getStarredPendingTasks();

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY starred DESC, priority ASC, created_at DESC")
    List<Task> getAllTasksByPriorityWithStarred();

    @Query("UPDATE tasks SET priority = 1 WHERE due_date < :now AND due_date IS NOT NULL AND status = 'pending' AND priority > 1")
    void updateOverdueTasksPriority(long now);
}
