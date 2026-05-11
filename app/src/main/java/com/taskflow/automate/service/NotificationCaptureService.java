package com.taskflow.automate.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.BadgeUtils;
import com.taskflow.automate.util.PreferenceManager;
import com.taskflow.automate.util.PriorityAssigner;
import com.taskflow.automate.util.ReminderScheduler;
import com.taskflow.automate.util.TaskExtractor;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationCaptureService extends NotificationListenerService {

    private static final String TAG = "NotificationCapture";

    private TaskExtractor taskExtractor;
    private PriorityAssigner priorityAssigner;
    private PreferenceManager preferenceManager;
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        taskExtractor = new TaskExtractor();
        priorityAssigner = new PriorityAssigner();
        preferenceManager = new PreferenceManager(this);
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        String packageName = sbn.getPackageName();

        // Skip our own notifications to prevent self-capture feedback loop
        if ("com.taskflow.automate".equals(packageName)) {
            return;
        }

        // Skip if app is in the blocked list
        if (isAppBlocked(packageName)) {
            return;
        }

        // Skip ongoing notifications (background services, music, etc.)
        if (sbn.isOngoing()) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }

        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String text = extras.getString(Notification.EXTRA_TEXT);

        // Skip if both title and text are empty
        if ((title == null || title.isEmpty()) && (text == null || text.isEmpty())) {
            return;
        }

        // Extract task information
        TaskExtractor.TaskExtractionResult result = taskExtractor.extractTask(title, text, packageName);

        if (!result.isActionable) {
            return;
        }

        // Create and persist the task
        String notificationKey = sbn.getKey();
        createAndPersistTask(result, packageName, notificationKey);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // No action needed when notifications are removed
    }

    private boolean isAppBlocked(String packageName) {
        Set<String> blockedApps = preferenceManager.getBlockedApps();
        return blockedApps.contains(packageName);
    }

    private void createAndPersistTask(TaskExtractor.TaskExtractionResult result,
                                       String packageName, String notificationKey) {
        executorService.execute(() -> {
            try {
                TaskDao taskDao = AppDatabase.getInstance(getApplicationContext()).taskDao();

                // Skip duplicate tasks with the same notification key
                if (notificationKey != null) {
                    Task existing = taskDao.getTaskByNotificationKey(notificationKey);
                    if (existing != null) {
                        return;
                    }
                }

                // Track this app as a known notification source
                preferenceManager.addKnownApp(packageName);

                Task task = new Task();
                task.setTitle(result.taskTitle);
                task.setDescription(result.taskDescription);
                task.setSourceApp(packageName);
                task.setDueDate(result.dueDateHint);
                task.setCreatedAt(System.currentTimeMillis());
                task.setStatus("pending");
                task.setNotificationKey(notificationKey);

                // Assign priority
                int priority = priorityAssigner.assignPriority(
                        packageName, result.taskTitle, result.taskDescription, result.dueDateHint);
                task.setPriority(priority);

                // Insert into database
                long taskId = taskDao.insertTask(task);
                task.setId(taskId);

                // Schedule reminder
                ReminderScheduler.scheduleReminder(getApplicationContext(), task);

                // Update badge count
                BadgeUtils.updateBadgeCount(getApplicationContext());

                Log.d(TAG, "Task created: " + task.getTitle() + " (priority: " + task.getPriorityLabel() + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error creating task", e);
            }
        });
    }
}
