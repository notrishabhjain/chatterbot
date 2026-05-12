package com.taskflow.automate.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.receiver.NotificationDismissReceiver;
import com.taskflow.automate.receiver.NotificationTaskReceiver;
import com.taskflow.automate.util.BadgeUtils;
import com.taskflow.automate.util.DuplicateTaskDetector;
import com.taskflow.automate.util.PreferenceManager;
import com.taskflow.automate.util.PriorityAssigner;
import com.taskflow.automate.util.ReminderScheduler;
import com.taskflow.automate.util.SelfForwardDetector;
import com.taskflow.automate.util.SmartTaskCategorizer;
import com.taskflow.automate.util.TaskExtractor;
import com.taskflow.automate.widget.TaskWidgetProvider;

import android.os.Parcelable;

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
    private static final String TASK_ACTION_CHANNEL_ID = "task_action_channel";

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

        // Load learned keywords from preferences
        Set<String> learnedKeywords = preferenceManager.getLearnedKeywords();
        if (!learnedKeywords.isEmpty()) {
            taskExtractor.setAdditionalKeywords(learnedKeywords);
        }
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

        // WhatsApp-specific: extract content from MessagingStyle EXTRA_MESSAGES
        String whatsAppSender = null;
        if (isWhatsAppPackage(packageName)) {
            Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (messages != null && messages.length > 0) {
                // Extract the last message's text and sender
                Parcelable lastMsg = messages[messages.length - 1];
                if (lastMsg instanceof Bundle) {
                    Bundle msgBundle = (Bundle) lastMsg;
                    CharSequence msgText = msgBundle.getCharSequence("text");
                    CharSequence msgSender = msgBundle.getCharSequence("sender");
                    if (msgText != null && msgText.length() > 0) {
                        text = msgText.toString();
                    }
                    if (msgSender != null && msgSender.length() > 0) {
                        title = msgSender.toString();
                        whatsAppSender = msgSender.toString();
                    }
                }
            }

            // Check for EXTRA_CONVERSATION_TITLE (group chat name)
            CharSequence conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            if (conversationTitle != null && conversationTitle.length() > 0) {
                title = conversationTitle.toString();
            }

            // Handle summary notifications with EXTRA_TEXT_LINES
            CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null && textLines.length > 0 && (text == null || text.isEmpty()
                    || text.matches("\\d+\\s*(new\\s*)?messages?.*"))) {
                StringBuilder combined = new StringBuilder();
                for (CharSequence line : textLines) {
                    if (line != null && line.length() > 0) {
                        if (combined.length() > 0) {
                            combined.append("\n");
                        }
                        combined.append(line);
                    }
                }
                if (combined.length() > 0) {
                    text = combined.toString();
                }
            }
        }

        // Prefer EXTRA_BIG_TEXT over EXTRA_TEXT when available and longer
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (bigText != null && bigText.length() > 0) {
            if (text == null || bigText.length() > text.length()) {
                text = bigText.toString();
            }
        }

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

        // WhatsApp Chat Monitor: filter notifications to only process the monitored chat
        boolean forceCreateTask = false;
        if (isWhatsAppPackage(packageName) && preferenceManager.isWhatsAppMonitorEnabled()) {
            String monitoredChat = preferenceManager.getWhatsAppMonitoredChat();
            if (monitoredChat != null && !monitoredChat.isEmpty()) {
                boolean isFromMonitoredChat = false;
                if (title != null && title.equalsIgnoreCase(monitoredChat)) {
                    isFromMonitoredChat = true;
                }
                CharSequence convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
                if (convTitle != null && convTitle.toString().equalsIgnoreCase(monitoredChat)) {
                    isFromMonitoredChat = true;
                }
                if (!isFromMonitoredChat) {
                    return;
                }
                // From monitored chat - force task creation (bypass keyword check)
                forceCreateTask = true;
            }
        }

        // Extract task information
        TaskExtractor.TaskExtractionResult result = taskExtractor.extractTask(title, text, packageName);

        if (!result.isActionable && !forceCreateTask) {
            return;
        }

        // If forced by WhatsApp monitor, ensure task fields are populated
        if (forceCreateTask && !result.isActionable) {
            result.isActionable = true;
            if (result.taskTitle == null || result.taskTitle.isEmpty()) {
                result.taskTitle = title != null ? title : "WhatsApp Task";
            }
            if (result.taskDescription == null || result.taskDescription.isEmpty()) {
                result.taskDescription = text != null ? text : "";
            }
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

        // WhatsApp self-forward detection - use whatsAppSender (not title) to avoid
        // matching against the group conversation title
        if (isWhatsAppPackage(packageName) && isSelfForwardedMessage(whatsAppSender)) {
            createSelfForwardTask(result, packageName, sbn.getKey(), text);
            return;
        }

        // Post companion notification with "Create Task" action button
        // When action button is enabled, let the user decide - do NOT auto-create the task
        if (preferenceManager.isTaskActionButtonEnabled()) {
            postTaskActionNotification(
                    result.taskTitle != null ? result.taskTitle : title,
                    text,
                    packageName);
            return;
        }

        // Auto-create task only when action button feature is disabled
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

    private boolean isWhatsAppPackage(String packageName) {
        return "com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName);
    }

    /**
     * Checks if the notification sender matches the user's own WhatsApp name (self-forward).
     * Uses the sender name from EXTRA_MESSAGES (not the conversation title) to avoid
     * false positives in group chats.
     */
    private boolean isSelfForwardedMessage(String sender) {
        String selfName = preferenceManager.getWhatsAppSelfName();
        return SelfForwardDetector.isSelfForwardedMessage(sender, selfName);
    }

    /**
     * Creates a high-priority task from a self-forwarded WhatsApp message.
     * Skips normal deduplication since the user intentionally forwarded the message.
     */
    private void createSelfForwardTask(TaskExtractor.TaskExtractionResult result,
                                        String packageName, String notificationKey, String text) {
        executorService.execute(() -> {
            try {
                TaskDao taskDao = AppDatabase.getInstance(getApplicationContext()).taskDao();

                // Track this app as a known notification source
                preferenceManager.addKnownApp(packageName);

                // Apply smart categorization
                String category = smartCategorizer.categorize(result.taskTitle, result.taskDescription);
                String categoryPrefix = "[Category: " + category + "]\n";
                String finalDescription = categoryPrefix + (result.taskDescription != null ? result.taskDescription : "");

                Task task = new Task();
                task.setTitle(result.taskTitle);
                task.setDescription(finalDescription);
                task.setSourceApp("WhatsApp (Self-Forward)");
                task.setDueDate(result.dueDateHint);
                task.setCreatedAt(System.currentTimeMillis());
                task.setStatus("pending");
                task.setNotificationKey(notificationKey);
                task.setPriority(1); // HIGH priority for self-forwarded messages

                // Insert into database
                long taskId = taskDao.insertTask(task);
                task.setId(taskId);

                // Schedule reminder
                ReminderScheduler.scheduleReminder(getApplicationContext(), task);

                // Update badge count
                BadgeUtils.updateBadgeCount(getApplicationContext());

                // Refresh widget
                TaskWidgetProvider.refreshWidget(getApplicationContext());

                Log.d(TAG, "Self-forward task created: " + task.getTitle() + " (HIGH priority)");
            } catch (Exception e) {
                Log.e(TAG, "Error creating self-forward task", e);
            }
        });
    }

    /**
     * Posts a companion notification with a "Create Task" action button for the captured notification.
     */
    private void postTaskActionNotification(String title, String text, String packageName) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    TASK_ACTION_CHANNEL_ID,
                    getString(R.string.task_action_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.task_action_channel_description));
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        // Generate unique notification ID based on content and time bucket (per minute)
        int notificationId = (packageName + title + System.currentTimeMillis() / 60000).hashCode();

        // Create "Create Task" action intent
        Intent createTaskIntent = new Intent(this, NotificationTaskReceiver.class);
        createTaskIntent.setAction(NotificationTaskReceiver.ACTION_CREATE_TASK);
        createTaskIntent.putExtra(NotificationTaskReceiver.EXTRA_TASK_TITLE, title);
        createTaskIntent.putExtra(NotificationTaskReceiver.EXTRA_TASK_DESCRIPTION, text != null ? text : "");
        createTaskIntent.putExtra(NotificationTaskReceiver.EXTRA_SOURCE_APP, packageName);
        createTaskIntent.putExtra(NotificationTaskReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent createTaskPendingIntent = PendingIntent.getBroadcast(
                this, notificationId, createTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create "Dismiss" action intent
        Intent dismissIntent = new Intent(this, NotificationDismissReceiver.class);
        dismissIntent.putExtra(NotificationTaskReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, notificationId + 1, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        String contentText = title != null ? title : "";
        if (text != null && !text.isEmpty()) {
            contentText = contentText + ": " + text;
        }
        // Truncate preview
        if (contentText.length() > 100) {
            contentText = contentText.substring(0, 97) + "...";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TASK_ACTION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle(getString(R.string.add_as_task_title))
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(60000) // Auto-dismiss after 60 seconds
                .addAction(android.R.drawable.ic_input_add,
                        getString(R.string.notification_action_create_task),
                        createTaskPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.notification_action_dismiss),
                        dismissPendingIntent);

        manager.notify(notificationId, builder.build());
    }
}
