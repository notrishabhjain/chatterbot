package com.taskflow.automate.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.taskflow.automate.database.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BadgeUtils {

    private static final String BADGE_CHANNEL_ID = "badge_channel";
    private static final String BADGE_CHANNEL_NAME = "Task Badge";
    private static final int BADGE_NOTIFICATION_ID = 99999;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void updateBadgeCount(Context context) {
        executor.execute(() -> {
            int count = AppDatabase.getInstance(context).taskDao().getTaskCountByStatus("pending");
            updateBadge(context, count);
        });
    }

    private static void updateBadge(Context context, int count) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BADGE_CHANNEL_ID, BADGE_CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);
        }

        if (count == 0) {
            manager.cancel(BADGE_NOTIFICATION_ID);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BADGE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("TaskFlow")
                .setContentText(count + " pending tasks")
                .setNumber(count)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(false)
                .setAutoCancel(false);

        manager.notify(BADGE_NOTIFICATION_ID, builder.build());
    }
}
