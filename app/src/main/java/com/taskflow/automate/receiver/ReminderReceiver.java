package com.taskflow.automate.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.ReminderScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "task_reminders";
    private static final String CHANNEL_NAME = "Task Reminders";
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
                TaskDao taskDao = AppDatabase.getInstance(context).taskDao();
                Task task = taskDao.getTaskById(taskId);

                if (task == null || !"pending".equals(task.getStatus())) {
                    return;
                }

                // Create notification channel
                createNotificationChannel(context);

                // Build and show notification
                showReminderNotification(context, task);

                // Reschedule for next reminder
                ReminderScheduler.scheduleReminder(context, task);
            } catch (Exception e) {
                // Silently handle database errors
            }
        });
        executor.shutdown();
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for pending tasks");
            channel.enableVibration(true);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showReminderNotification(Context context, Task task) {
        // Create "Mark Complete" action intent
        Intent completeIntent = new Intent(context, TaskCompleteReceiver.class);
        completeIntent.putExtra(EXTRA_TASK_ID, task.getId());
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = "Priority: " + task.getPriorityLabel() + " - " + task.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(task.getTitle())
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        "Mark Complete", completePendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) task.getId(), builder.build());
        }
    }
}
