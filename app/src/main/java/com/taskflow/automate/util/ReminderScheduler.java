package com.taskflow.automate.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.taskflow.automate.model.Task;
import com.taskflow.automate.receiver.ReminderReceiver;

public class ReminderScheduler {

    private static final String ACTION_TASK_REMINDER = "com.taskflow.automate.ACTION_TASK_REMINDER";
    private static final String EXTRA_TASK_ID = "extra_task_id";

    private static final long HIGH_PRIORITY_INTERVAL = 30 * 60 * 1000L;    // 30 minutes
    private static final long MEDIUM_PRIORITY_INTERVAL = 60 * 60 * 1000L;  // 60 minutes
    private static final long LOW_PRIORITY_INTERVAL = 120 * 60 * 1000L;    // 120 minutes

    public static void scheduleReminder(Context context, Task task) {
        long interval = getReminderInterval(context, task.getPriority());
        long triggerTime = System.currentTimeMillis() + interval;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, task.getId());

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );
    }

    public static void cancelReminder(Context context, long taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, taskId);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static long getReminderInterval(Context context, int priority) {
        PreferenceManager prefManager = new PreferenceManager(context);
        int minutes = prefManager.getReminderInterval(priority);
        return minutes * 60 * 1000L;
    }

    private static PendingIntent createPendingIntent(Context context, long taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_TASK_REMINDER);
        intent.putExtra(EXTRA_TASK_ID, taskId);

        return PendingIntent.getBroadcast(
                context,
                (int) taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
