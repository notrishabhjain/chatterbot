package com.taskflow.automate.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.BadgeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuickAddReceiver extends BroadcastReceiver {

    public static final String ACTION_QUICK_ADD = "com.taskflow.automate.ACTION_QUICK_ADD";
    public static final String KEY_QUICK_ADD_TEXT = "quick_add_text";
    private static final String CHANNEL_ID = "quick_add_channel";
    private static final int NOTIFICATION_ID = 88888;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) return;

        CharSequence inputText = remoteInput.getCharSequence(KEY_QUICK_ADD_TEXT);
        if (inputText == null || inputText.toString().trim().isEmpty()) return;

        String taskTitle = inputText.toString().trim();

        final PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Task task = new Task();
                task.setTitle(taskTitle);
                task.setDescription("");
                task.setSourceApp("Quick Add");
                task.setPriority(2);
                task.setStatus("pending");
                task.setCreatedAt(System.currentTimeMillis());

                AppDatabase.getInstance(context).taskDao().insertTask(task);
                BadgeUtils.updateBadgeCount(context);

                // Re-show the quick add notification to allow more input
                showQuickAddNotification(context);
            } finally {
                pendingResult.finish();
            }
        });
        executor.shutdown();
    }

    public static void showQuickAddNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Quick Add Tasks", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Persistent notification for quickly adding tasks");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        // Create RemoteInput for direct reply
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_QUICK_ADD_TEXT)
                .setLabel("Add a task...")
                .build();

        Intent replyIntent = new Intent(context, QuickAddReceiver.class);
        replyIntent.setAction(ACTION_QUICK_ADD);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                context, 0, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_input_add,
                "Add Task",
                replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(false)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("TaskFlow Quick Add")
                .setContentText("Type to add a new task")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(action);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void dismissQuickAddNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
