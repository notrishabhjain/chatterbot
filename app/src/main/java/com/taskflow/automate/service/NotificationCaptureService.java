package com.taskflow.automate.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
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
        String title = getStringExtra(extras, Notification.EXTRA_TITLE);
        String text = getStringExtra(extras, Notification.EXTRA_TEXT);
        String bigText = getStringExtra(extras, Notification.EXTRA_BIG_TEXT);
        String subText = getStringExtra(extras, Notification.EXTRA_SUB_TEXT);
        String infoText = getStringExtra(extras, Notification.EXTRA_INFO_TEXT);

        // Skip if all text fields are empty
        if (isEmpty(title) && isEmpty(text) && isEmpty(bigText) && isEmpty(subText) && isEmpty(infoText)) {
            return;
        }

        // Combine subText and infoText into the subText parameter for extraction
        String combinedSubText = combineSubTexts(subText, infoText);

        // Extract task information using weighted scoring
        TaskExtractor.TaskExtractionResult result =
                taskExtractor.extractTask(title, text, bigText, combinedSubText, packageName);

        Log.d(TAG, "Notification from " + packageName + " scored: " + result.actionabilityScore
                + " (threshold: 25)");

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

    private String getStringExtra(Bundle extras, String key) {
        CharSequence cs = extras.getCharSequence(key);
        return cs != null ? cs.toString() : null;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private String combineSubTexts(String subText, String infoText) {
        if (isEmpty(subText) && isEmpty(infoText)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(subText)) {
            sb.append(subText);
        }
        if (!isEmpty(infoText)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(infoText);
        }
        return sb.toString();
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
                task.setAssigner(result.assigner);
                task.setTaskType(result.taskType);
                task.setFollowUp(result.isFollowUp);
                task.setSourceNotificationText(result.sourceNotificationText);

                // Assign priority (with task type factor)
                int priority = priorityAssigner.assignPriority(
                        packageName, result.taskTitle, result.taskDescription,
                        result.dueDateHint, result.taskType);
                task.setPriority(priority);

                // Insert into database
                long taskId = taskDao.insertTask(task);
                task.setId(taskId);

                // Schedule reminder
                ReminderScheduler.scheduleReminder(getApplicationContext(), task);

                Log.d(TAG, "Task created: " + task.getTitle()
                        + " (priority: " + task.getPriorityLabel()
                        + ", type: " + task.getTaskType()
                        + ", score: " + result.actionabilityScore + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error creating task", e);
            }
        });
    }
}
