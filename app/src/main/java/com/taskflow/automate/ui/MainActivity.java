package com.taskflow.automate.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.database.TaskDao;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.service.NotificationCaptureService;
import com.taskflow.automate.util.ReminderScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskCompleteListener {

    private static final int VIEW_PRIORITY = 0;
    private static final int VIEW_DEADLINE = 1;
    private static final int VIEW_ASSIGNER = 2;
    private static final int VIEW_FOLLOW_UP = 3;

    private RecyclerView recyclerTasks;
    private TextView textEmptyState;
    private TabLayout tabLayout;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private int currentViewMode = VIEW_PRIORITY;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Permission result handled; no further action needed
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupTabLayout();
        setupRecyclerView();
        checkNotificationListenerPermission();
        requestPostNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupTabLayout() {
        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_priority));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_deadline));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_assigner));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_follow_up));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentViewMode = tab.getPosition();
                loadTasks();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No action needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Refresh on reselect
                loadTasks();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerTasks = findViewById(R.id.recycler_tasks);
        textEmptyState = findViewById(R.id.text_empty_state);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void loadTasks() {
        executor.execute(() -> {
            TaskDao taskDao = AppDatabase.getInstance(this).taskDao();
            List<Task> tasks;
            switch (currentViewMode) {
                case VIEW_DEADLINE:
                    tasks = taskDao.getAllTasksByDeadline();
                    break;
                case VIEW_ASSIGNER:
                    tasks = loadTasksGroupedByAssigner(taskDao);
                    break;
                case VIEW_FOLLOW_UP:
                    tasks = taskDao.getFollowUpTasks();
                    break;
                case VIEW_PRIORITY:
                default:
                    tasks = taskDao.getAllTasksByPriority();
                    break;
            }
            runOnUiThread(() -> {
                taskList.clear();
                taskList.addAll(tasks);
                taskAdapter.updateTasks(taskList);
                updateEmptyState();
            });
        });
    }

    private List<Task> loadTasksGroupedByAssigner(TaskDao taskDao) {
        List<Task> grouped = new ArrayList<>();
        List<String> assigners = taskDao.getAllAssigners();
        for (String assigner : assigners) {
            grouped.addAll(taskDao.getTasksByAssigner(assigner));
        }
        // Add tasks with no assigner at the end
        List<Task> allTasks = taskDao.getAllTasksByPriority();
        for (Task task : allTasks) {
            if (task.getAssigner() == null || task.getAssigner().isEmpty()) {
                grouped.add(task);
            }
        }
        return grouped;
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerTasks.setVisibility(View.GONE);
            // Set contextual empty state message
            switch (currentViewMode) {
                case VIEW_DEADLINE:
                    textEmptyState.setText(R.string.empty_state_deadline);
                    break;
                case VIEW_ASSIGNER:
                    textEmptyState.setText(R.string.empty_state_assigner);
                    break;
                case VIEW_FOLLOW_UP:
                    textEmptyState.setText(R.string.empty_state_follow_up);
                    break;
                case VIEW_PRIORITY:
                default:
                    textEmptyState.setText(R.string.empty_state_message);
                    break;
            }
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskComplete(Task task, int position) {
        executor.execute(() -> {
            AppDatabase.getInstance(this).taskDao().markComplete(task.getId());
            ReminderScheduler.cancelReminder(this, task.getId());
            runOnUiThread(() -> {
                taskAdapter.removeTask(position);
                updateEmptyState();
            });
        });
    }

    private void checkNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notification_permission_title)
                    .setMessage(R.string.notification_permission_message)
                    .setPositiveButton(R.string.enable, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName componentName = new ComponentName(this, NotificationCaptureService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(componentName.flattenToString());
    }

    private void requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
