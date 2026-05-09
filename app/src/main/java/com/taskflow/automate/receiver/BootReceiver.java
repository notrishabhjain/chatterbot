package com.taskflow.automate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.ReminderScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                TaskDao taskDao = AppDatabase.getInstance(context).taskDao();
                List<Task> pendingTasks = taskDao.getPendingTasks();

                for (Task task : pendingTasks) {
                    ReminderScheduler.scheduleReminder(context, task);
                }
            } catch (Exception e) {
                // Silently handle errors during boot rescheduling
            }
        });
        executor.shutdown();
    }
}
