package com.taskflow.automate.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.BadgeUtils;
import com.taskflow.automate.widget.TaskWidgetProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationTaskReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationTaskReceiver";
    public static final String ACTION_CREATE_TASK = "com.taskflow.automate.ACTION_CREATE_TASK_FROM_NOTIFICATION";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_DESCRIPTION = "task_description";
    public static final String EXTRA_SOURCE_APP = "source_app";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_CREATE_TASK.equals(intent.getAction())) {
            return;
        }

        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        String taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION);
        String sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        if (taskTitle == null || taskTitle.isEmpty()) {
            return;
        }

        // Cancel the companion notification
        if (notificationId != -1) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(notificationId);
            }
        }

        final PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Task task = new Task();
                task.setTitle(taskTitle);
                task.setDescription(taskDescription != null ? taskDescription : "");
                task.setSourceApp(sourceApp != null ? sourceApp : "Notification");
                task.setPriority(2);
                task.setStatus("pending");
                task.setCreatedAt(System.currentTimeMillis());

                AppDatabase.getInstance(context).taskDao().insertTask(task);
                BadgeUtils.updateBadgeCount(context);
                TaskWidgetProvider.refreshWidget(context);

                Log.d(TAG, "Task created from notification action: " + taskTitle);
            } catch (Exception e) {
                Log.e(TAG, "Error creating task from notification", e);
            } finally {
                pendingResult.finish();
            }
        });
        executor.shutdown();
    }
}
