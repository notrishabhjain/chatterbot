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
import com.taskflow.automate.util.DuplicateTaskDetector;
import com.taskflow.automate.util.PreferenceManager;
import com.taskflow.automate.util.PriorityAssigner;
import com.taskflow.automate.util.ReminderScheduler;
import com.taskflow.automate.util.SmartTaskCategorizer;
import com.taskflow.automate.util.TaskExtractor;
import com.taskflow.automate.widget.TaskWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationCaptureService extends NotificationListenerService {

    private static final String TAG = "NotificationCapture";
    private static final long DEDUP_WINDOW_MS = 5 * 60 * 1000L; // 5 minutes

    private static final Set<String> MESSAGING_APPS = new HashSet<>(Arrays.asList(
            "com.whatsapp", "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.slack", "com.Slack",
            "com.microsoft.teams"
    ));

    private static final Set<String> EMAIL_APPS = new HashSet<>(Arrays.asList(
            "com.google.android.gm",
            "com.microsoft.office.outlook"
    ));

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\S]+");

    private TaskExtractor taskExtractor;
    private PriorityAssigner priorityAssigner;
    private SmartTaskCategorizer smartCategorizer;
    private DuplicateTaskDetector duplicateDetector;
    private PreferenceManager preferenceManager;
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, DeduplicationEntry> deduplicationMap = new ConcurrentHashMap<>();

    private static class DeduplicationEntry {
        long taskId;
        long timestamp;

        DeduplicationEntry(long taskId, long timestamp) {
            this.taskId = taskId;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        taskExtractor = new TaskExtractor();
        priorityAssigner = new PriorityAssigner();
        smartCategorizer = new SmartTaskCategorizer();
        duplicateDetector = new DuplicateTaskDetector();
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

        // For messaging apps: use title as sender/assignee
        String senderName = null;
        if (MESSAGING_APPS.contains(packageName)) {
            senderName = title;
        }

        // For email apps: title is the sender, text has subject - swap BEFORE building description
        if (EMAIL_APPS.contains(packageName)) {
            senderName = title;
            // Use text as title (subject line) for email notifications
            if (text != null && !text.isEmpty()) {
                title = text;
            }
        }

        // Extract richer context from notification extras (after email swap so text/title are correct)
        String enhancedDescription = buildEnhancedDescription(text, extras);

        // Skip if both title and text are empty
        if ((title == null || title.isEmpty()) && (text == null || text.isEmpty())) {
            return;
        }

        // Extract task information
        TaskExtractor.TaskExtractionResult result = taskExtractor.extractTask(title, text, packageName);

        if (!result.isActionable) {
            return;
        }

        // Apply enhanced description if richer than original
        if (enhancedDescription != null && !enhancedDescription.isEmpty()) {
            if (result.taskDescription == null || enhancedDescription.length() > result.taskDescription.length()) {
                result.taskDescription = enhancedDescription;
            }
        }

        // Detect and append URLs found in notification text
        String urls = extractUrls(text);
        if (urls != null && !urls.isEmpty()) {
            String desc = result.taskDescription != null ? result.taskDescription : "";
            result.taskDescription = desc + "\n" + urls;
        }

        // Clean up old entries before dedup check
        cleanupDeduplicationMap();

        // Bound the map size to prevent unbounded growth
        if (deduplicationMap.size() > 100) {
            deduplicationMap.clear();
        }

        // Smart deduplication - check if same sender sent within 5 minutes
        String deduplicationKey = packageName + "|" + (title != null ? title : "");
        DeduplicationEntry existingEntry = deduplicationMap.get(deduplicationKey);
        long now = System.currentTimeMillis();

        if (existingEntry != null && (now - existingEntry.timestamp) < DEDUP_WINDOW_MS) {
            // Append to existing task instead of creating new one
            appendToExistingTask(existingEntry.taskId, text, now);
            existingEntry.timestamp = now; // Update timestamp
            return;
        }

        // Create and persist the task
        String notificationKey = sbn.getKey();
        createAndPersistTask(result, packageName, notificationKey, deduplicationKey, senderName);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // No action needed when notifications are removed
    }

    private boolean isAppBlocked(String packageName) {
        Set<String> blockedApps = preferenceManager.getBlockedApps();
        return blockedApps.contains(packageName);
    }

    /**
     * Builds an enhanced description by extracting additional context from notification extras.
     */
    private String buildEnhancedDescription(String baseText, Bundle extras) {
        StringBuilder desc = new StringBuilder();

        if (baseText != null && !baseText.isEmpty()) {
            desc.append(baseText);
        }

        // Extract text lines (available in bundled/expanded notifications)
        CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (textLines != null && textLines.length > 0) {
            for (CharSequence line : textLines) {
                if (line != null && line.length() > 0) {
                    if (desc.length() > 0) {
                        desc.append("\n");
                    }
                    desc.append(line);
                }
            }
        }

        // Extract sub text
        String subText = extras.getString(Notification.EXTRA_SUB_TEXT);
        if (subText != null && !subText.isEmpty()) {
            if (desc.length() > 0) {
                desc.append("\n");
            }
            desc.append(subText);
        }

        return desc.toString();
    }

    /**
     * Extracts URLs from the notification text using a regex pattern.
     */
    private String extractUrls(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder urls = new StringBuilder();
        while (matcher.find()) {
            if (urls.length() > 0) {
                urls.append("\n");
            }
            urls.append("Link: ").append(matcher.group());
        }
        return urls.length() > 0 ? urls.toString() : null;
    }

    private void createAndPersistTask(TaskExtractor.TaskExtractionResult result,
                                       String packageName, String notificationKey,
                                       String deduplicationKey, String senderName) {
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

                // Duplicate detection: check against existing pending tasks
                List<Task> pendingTasks = taskDao.getPendingTasks();
                Task duplicate = duplicateDetector.findDuplicate(result.taskTitle, result.taskDescription, pendingTasks);
                if (duplicate != null) {
                    // Append new notification text to existing duplicate task
                    String existingDesc = duplicate.getDescription() != null ? duplicate.getDescription() : "";
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String timeStr = sdf.format(new Date(System.currentTimeMillis()));
                    String newInfo = result.taskDescription != null ? result.taskDescription : "";
                    // Cap description length to prevent unbounded growth
                    if (existingDesc.length() > 2000) {
                        existingDesc = existingDesc.substring(existingDesc.length() - 1500);
                    }
                    String updatedDesc = existingDesc + "\n[" + timeStr + "] " + newInfo;
                    duplicate.setDescription(updatedDesc);
                    taskDao.updateTask(duplicate);
                    return;
                }

                // Track this app as a known notification source
                preferenceManager.addKnownApp(packageName);

                // Apply smart categorization
                String category = smartCategorizer.categorize(result.taskTitle, result.taskDescription);
                String categoryPrefix = "[Category: " + category + "]\n";
                String finalDescription = categoryPrefix + (result.taskDescription != null ? result.taskDescription : "");

                Task task = new Task();
                task.setTitle(result.taskTitle);
                task.setDescription(finalDescription);
                task.setSourceApp(packageName);
                task.setDueDate(result.dueDateHint);
                task.setCreatedAt(System.currentTimeMillis());
                task.setStatus("pending");
                task.setNotificationKey(notificationKey);

                // Set assignee from sender name (messaging/email apps)
                if (senderName != null && !senderName.isEmpty()) {
                    task.setAssignee(senderName);
                }

                // Assign priority - use the higher priority between PriorityAssigner and SmartTaskCategorizer
                int priorityFromAssigner = priorityAssigner.assignPriority(
                        packageName, result.taskTitle, result.taskDescription, result.dueDateHint);
                int priorityFromCategorizer = smartCategorizer.suggestPriority(
                        result.taskTitle, result.taskDescription, result.dueDateHint);
                int priority = Math.min(priorityFromAssigner, priorityFromCategorizer);
                task.setPriority(priority);

                // Insert into database
                long taskId = taskDao.insertTask(task);
                task.setId(taskId);

                // Track for deduplication
                deduplicationMap.put(deduplicationKey, new DeduplicationEntry(taskId, System.currentTimeMillis()));

                // Schedule reminder
                ReminderScheduler.scheduleReminder(getApplicationContext(), task);

                // Update badge count
                BadgeUtils.updateBadgeCount(getApplicationContext());

                // Refresh widget to show new task
                TaskWidgetProvider.refreshWidget(getApplicationContext());

                Log.d(TAG, "Task created: " + task.getTitle() + " (priority: " + task.getPriorityLabel() + ", category: " + category + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error creating task", e);
            }
        });
    }

    private void appendToExistingTask(long taskId, String newText, long timestamp) {
        if (newText == null || newText.isEmpty()) return;
        executorService.execute(() -> {
            try {
                TaskDao taskDao = AppDatabase.getInstance(getApplicationContext()).taskDao();
                Task task = taskDao.getTaskById(taskId);
                if (task != null && "pending".equals(task.getStatus())) {
                    String existingDesc = task.getDescription() != null ? task.getDescription() : "";
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String timeStr = sdf.format(new Date(timestamp));
                    // Cap description length to prevent unbounded growth
                    if (existingDesc.length() > 2000) {
                        existingDesc = existingDesc.substring(existingDesc.length() - 1500);
                    }
                    String updatedDesc = existingDesc + "\n[" + timeStr + "] " + newText;
                    task.setDescription(updatedDesc);
                    taskDao.updateTask(task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error appending to task", e);
            }
        });
    }

    private void cleanupDeduplicationMap() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, DeduplicationEntry> entry : deduplicationMap.entrySet()) {
            if (now - entry.getValue().timestamp > DEDUP_WINDOW_MS) {
                deduplicationMap.remove(entry.getKey());
            }
        }
    }
}
