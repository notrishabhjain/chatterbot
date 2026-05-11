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
import com.taskflow.automate.ui.TaskEditActivity;
import com.taskflow.automate.util.RecurringTaskManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_COMPLETE_TASK = "com.taskflow.automate.WIDGET_COMPLETE_TASK";
    public static final String ACTION_VIEW_TASK = "com.taskflow.automate.WIDGET_VIEW_TASK";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        // Force data refresh on update
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
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

        // Set up pending intent template for list item clicks
        Intent templateIntent = new Intent(context, TaskWidgetProvider.class);
        PendingIntent templatePending = PendingIntent.getBroadcast(context, 0, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, templatePending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_VIEW_TASK.equals(action)) {
            long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
            if (taskId != -1) {
                Intent viewIntent = new Intent(context, TaskEditActivity.class);
                viewIntent.putExtra(TaskEditActivity.EXTRA_TASK_ID, taskId);
                viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(viewIntent);
            }
        } else if (ACTION_COMPLETE_TASK.equals(action)) {
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
