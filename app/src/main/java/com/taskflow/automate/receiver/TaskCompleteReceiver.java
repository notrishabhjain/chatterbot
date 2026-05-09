package com.taskflow.automate.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.util.ReminderScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskCompleteReceiver extends BroadcastReceiver {

    private static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Mark task as complete in database
                TaskDao taskDao = AppDatabase.getInstance(context).taskDao();
                taskDao.markComplete(taskId);

                // Cancel future reminders
                ReminderScheduler.cancelReminder(context, taskId);

                // Cancel the notification
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel((int) taskId);
                }
            } catch (Exception e) {
                // Silently handle errors
            }
        });
        executor.shutdown();
    }
}
