package com.taskflow.automate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.ReminderScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    private static final int MAX_REMINDERS_HIGH = 20;
    private static final int MAX_REMINDERS_MEDIUM = 15;
    private static final int MAX_REMINDERS_LOW = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        final PendingResult pendingResult = goAsync();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                TaskDao taskDao = AppDatabase.getInstance(context).taskDao();
                List<Task> pendingTasks = taskDao.getPendingTasks();

                for (Task task : pendingTasks) {
                    int maxReminders = getMaxReminders(task.getPriority());
                    if (task.getReminderCount() >= maxReminders) {
                        continue;
                    }
                    ReminderScheduler.scheduleReminder(context, task);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling reminders on boot", e);
            } finally {
                pendingResult.finish();
            }
        });
        executor.shutdown();
    }

    private int getMaxReminders(int priority) {
        switch (priority) {
            case 1:
                return MAX_REMINDERS_HIGH;
            case 2:
                return MAX_REMINDERS_MEDIUM;
            case 3:
                return MAX_REMINDERS_LOW;
            default:
                return MAX_REMINDERS_LOW;
        }
    }
}
