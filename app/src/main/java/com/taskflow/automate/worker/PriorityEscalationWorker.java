package com.taskflow.automate.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taskflow.automate.database.AppDatabase;

import java.util.concurrent.TimeUnit;

public class PriorityEscalationWorker extends Worker {

    private static final String WORK_NAME = "priority_escalation";

    public PriorityEscalationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            long now = System.currentTimeMillis();
            AppDatabase.getInstance(getApplicationContext()).taskDao().updateOverdueTasksPriority(now);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    public static void scheduleEscalation(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PriorityEscalationWorker.class, 6, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
