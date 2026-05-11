package com.taskflow.automate.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WeeklySummaryWorker extends Worker {

    private static final String WORK_NAME = "weekly_summary";
    private static final String CHANNEL_ID = "weekly_summary";
    private static final String CHANNEL_NAME = "Weekly Summary";
    private static final int NOTIFICATION_ID = 77777;

    public WeeklySummaryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            TaskDao taskDao = AppDatabase.getInstance(context).taskDao();

            int pendingCount = taskDao.getTaskCountByStatus("pending");
            int overdueCount = taskDao.getOverdueCount(System.currentTimeMillis());

            // Completed this week
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long weekStart = cal.getTimeInMillis();
            long weekEnd = System.currentTimeMillis();
            int completedThisWeek = taskDao.getCompletedThisWeekCount(weekStart, weekEnd);

            // Create notification
            createNotificationChannel(context);
            showSummaryNotification(context, pendingCount, overdueCount, completedThisWeek);

            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Weekly task summary notifications");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showSummaryNotification(Context context, int pending, int overdue, int completed) {
        String body = "You have " + pending + " pending tasks, " + overdue + " overdue, " + completed + " completed this week";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Weekly Task Summary")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static void scheduleWeeklySummary(Context context) {
        // Calculate initial delay to next Sunday 6PM
        Calendar now = Calendar.getInstance();
        Calendar nextSunday = Calendar.getInstance();
        nextSunday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        nextSunday.set(Calendar.HOUR_OF_DAY, 18);
        nextSunday.set(Calendar.MINUTE, 0);
        nextSunday.set(Calendar.SECOND, 0);
        nextSunday.set(Calendar.MILLISECOND, 0);

        // If we're past Sunday 6PM this week, move to next week
        if (nextSunday.getTimeInMillis() <= now.getTimeInMillis()) {
            nextSunday.add(Calendar.WEEK_OF_YEAR, 1);
        }

        long initialDelay = nextSunday.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WeeklySummaryWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
