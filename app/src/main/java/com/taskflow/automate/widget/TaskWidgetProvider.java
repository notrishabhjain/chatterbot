package com.taskflow.automate.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.MainActivity;
import com.taskflow.automate.util.RecurringTaskManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_COMPLETE_TASK = "com.taskflow.automate.WIDGET_COMPLETE_TASK";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_list);

        // Set up header click to open app
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPending = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_header, mainPending);

        // Set up RemoteViewsService for list
        Intent serviceIntent = new Intent(context, TaskWidgetService.class);
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);

        // Set up pending intent template for list item clicks (complete action)
        Intent completeIntent = new Intent(context, TaskWidgetProvider.class);
        completeIntent.setAction(ACTION_COMPLETE_TASK);
        PendingIntent completePending = PendingIntent.getBroadcast(context, 0, completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, completePending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_COMPLETE_TASK.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
            if (taskId != -1) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    TaskDao taskDao = AppDatabase.getInstance(context).taskDao();
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null) {
                        taskDao.markCompleteWithTimestamp(taskId, System.currentTimeMillis());

                        // Handle recurring tasks
                        RecurringTaskManager recurringManager = new RecurringTaskManager();
                        Task nextTask = recurringManager.createNextRecurrence(task);
                        if (nextTask != null) {
                            taskDao.insertTask(nextTask);
                        }
                    }
                    // Refresh widget
                    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                    int[] ids = widgetManager.getAppWidgetIds(
                            new ComponentName(context, TaskWidgetProvider.class));
                    widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
                });
                executor.shutdown();
            }
        }
    }
}
