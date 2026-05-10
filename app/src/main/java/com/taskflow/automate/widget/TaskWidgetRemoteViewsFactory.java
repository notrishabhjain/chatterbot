package com.taskflow.automate.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<Task> tasks = new ArrayList<>();

    public TaskWidgetRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // Initial data load happens in onDataSetChanged
    }

    @Override
    public void onDataSetChanged() {
        List<Task> allTasks = AppDatabase.getInstance(context).taskDao().getAllTasksByPriority();
        tasks.clear();
        int limit = Math.min(allTasks.size(), 5);
        for (int i = 0; i < limit; i++) {
            tasks.add(allTasks.get(i));
        }
    }

    @Override
    public void onDestroy() {
        tasks.clear();
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= tasks.size()) {
            return null;
        }

        Task task = tasks.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);

        views.setTextViewText(R.id.widget_task_title, task.getTitle());

        // Set priority color
        int priorityColor;
        switch (task.getPriority()) {
            case 1:
                priorityColor = Color.parseColor("#F44336");
                break;
            case 2:
                priorityColor = Color.parseColor("#FF9800");
                break;
            case 3:
            default:
                priorityColor = Color.parseColor("#4CAF50");
                break;
        }
        views.setInt(R.id.widget_priority_bar, "setBackgroundColor", priorityColor);

        // Set fill-in intent with task ID for the complete action
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.getId());
        views.setOnClickFillInIntent(R.id.widget_btn_complete, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= tasks.size()) {
            return 0;
        }
        return tasks.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
