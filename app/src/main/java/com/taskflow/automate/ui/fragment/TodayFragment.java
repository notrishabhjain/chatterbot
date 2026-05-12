package com.taskflow.automate.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.SwipeCallback;
import com.taskflow.automate.ui.TaskAdapter;
import com.taskflow.automate.ui.TaskEditActivity;
import com.taskflow.automate.util.BadgeUtils;
import com.taskflow.automate.util.RecurringTaskManager;
import com.taskflow.automate.util.ReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodayFragment extends Fragment implements TaskAdapter.OnTaskCompleteListener,
        TaskAdapter.OnTaskClickListener, SwipeCallback.SwipeActionListener,
        TaskAdapter.OnTaskStarListener {

    private RecyclerView recyclerToday;
    private TextView textEmptyToday;
    private TextView textStatPending;
    private TextView textStatHighPriority;
    private TextView textStatDueToday;
    private TextView textStatOverdue;
    private TextView textStatCompletedWeek;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TodayFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerToday = view.findViewById(R.id.recycler_today);
        textEmptyToday = view.findViewById(R.id.text_empty_today);

        // Stats views
        textStatPending = view.findViewById(R.id.text_stat_pending);
        textStatHighPriority = view.findViewById(R.id.text_stat_high_priority);
        textStatDueToday = view.findViewById(R.id.text_stat_due_today);
        textStatOverdue = view.findViewById(R.id.text_stat_overdue);
        textStatCompletedWeek = view.findViewById(R.id.text_stat_completed_week);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        taskAdapter.setOnTaskClickListener(this);
        taskAdapter.setOnTaskStarListener(this);
        recyclerToday.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerToday.setAdapter(taskAdapter);

        // Setup swipe gestures
        SwipeCallback swipeCallback = new SwipeCallback(this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerToday);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
        loadStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Do NOT shut down executor here - pending Snackbar callbacks may still need it
        // to persist task completions to the database. The executor will be GC'd naturally.
    }

    private void loadStats() {
        final android.content.Context ctx = getContext();
        if (ctx == null) return;
        final android.content.Context appContext = ctx.getApplicationContext();
        try {
            executor.execute(() -> {
                int pendingCount = AppDatabase.getInstance(appContext)
                        .taskDao().getTaskCountByStatus("pending");
                int highPriorityCount = AppDatabase.getInstance(appContext)
                        .taskDao().getHighPriorityPendingCount();

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long startOfDay = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                long endOfDay = cal.getTimeInMillis();

                int dueTodayCount = AppDatabase.getInstance(appContext)
                        .taskDao().getDueTodayCount(startOfDay, endOfDay);
                int overdueCount = AppDatabase.getInstance(appContext)
                        .taskDao().getOverdueCount(System.currentTimeMillis());

                // Week stats
                cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long weekStart = cal.getTimeInMillis();
                long weekEnd = System.currentTimeMillis();
                int completedThisWeek = AppDatabase.getInstance(appContext)
                        .taskDao().getCompletedThisWeekCount(weekStart, weekEnd);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        textStatPending.setText(String.valueOf(pendingCount));
                        textStatHighPriority.setText(String.valueOf(highPriorityCount));
                        textStatDueToday.setText(String.valueOf(dueTodayCount));
                        textStatOverdue.setText(String.valueOf(overdueCount));
                        textStatCompletedWeek.setText(getString(R.string.stat_completed_this_week, completedThisWeek));
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor shut down, ignore load request
        }
    }

    private void loadTasks() {
        final android.content.Context ctx = getContext();
        if (ctx == null) return;
        final android.content.Context appContext = ctx.getApplicationContext();
        try {
            executor.execute(() -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                long endOfDay = calendar.getTimeInMillis();

                long now = System.currentTimeMillis();

                List<Task> todayTasks = AppDatabase.getInstance(appContext)
                        .taskDao().getTasksDueToday(startOfDay, endOfDay);
                List<Task> overdueTasks = AppDatabase.getInstance(appContext)
                        .taskDao().getOverdueTasks(now);

                List<Task> combinedTasks = new ArrayList<>();
                combinedTasks.addAll(overdueTasks);
                combinedTasks.addAll(todayTasks);

                // Pre-load subtask counts
                Map<Long, int[]> countMap = new HashMap<>();
                for (Task t : combinedTasks) {
                    int total = AppDatabase.getInstance(appContext).subtaskDao().getTotalSubtaskCount(t.getId());
                    if (total > 0) {
                        int completed = AppDatabase.getInstance(appContext).subtaskDao().getCompletedSubtaskCount(t.getId());
                        countMap.put(t.getId(), new int[]{completed, total});
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        taskList.clear();
                        taskList.addAll(combinedTasks);
                        taskAdapter.setSubtaskCountMap(countMap);
                        taskAdapter.updateTasks(taskList);
                        updateEmptyState();
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor shut down, ignore load request
        }
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            textEmptyToday.setVisibility(View.VISIBLE);
            recyclerToday.setVisibility(View.GONE);
        } else {
            textEmptyToday.setVisibility(View.GONE);
            recyclerToday.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskComplete(Task task, int position) {
        performUndoableComplete(task, position);
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskEditActivity.class);
        intent.putExtra(TaskEditActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onSwipeLeft(int position) {
        Task task = taskAdapter.getTaskAtPosition(position);
        if (task != null) {
            performUndoableComplete(task, position);
        }
    }

    @Override
    public void onSwipeRight(int position) {
        Task task = taskAdapter.getTaskAtPosition(position);
        if (task != null) {
            snoozeTask(task, position);
        }
    }

    private void performUndoableComplete(Task task, int position) {
        taskAdapter.removeTask(position);
        updateEmptyState();

        final android.content.Context appContext = requireContext().getApplicationContext();

        Snackbar snackbar = Snackbar.make(requireView(), R.string.task_completed_message, 5000);
        snackbar.setAction(R.string.undo, v -> {
            taskAdapter.addTask(position, task);
            updateEmptyState();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    try {
                        executor.execute(() -> {
                            AppDatabase.getInstance(appContext)
                                    .taskDao().markCompleteWithTimestamp(task.getId(), System.currentTimeMillis());
                            ReminderScheduler.cancelReminder(appContext, task.getId());
                            BadgeUtils.updateBadgeCount(appContext);

                            // Handle recurring tasks
                            RecurringTaskManager recurringManager = new RecurringTaskManager();
                            Task nextTask = recurringManager.createNextRecurrence(task);
                            if (nextTask != null) {
                                AppDatabase.getInstance(appContext).taskDao().insertTask(nextTask);
                            }
                        });
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                        // Executor was shut down - use a temporary executor to ensure DB write completes
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(appContext)
                                    .taskDao().markCompleteWithTimestamp(task.getId(), System.currentTimeMillis());
                            ReminderScheduler.cancelReminder(appContext, task.getId());
                            BadgeUtils.updateBadgeCount(appContext);

                            RecurringTaskManager recurringManager = new RecurringTaskManager();
                            Task nextTask = recurringManager.createNextRecurrence(task);
                            if (nextTask != null) {
                                AppDatabase.getInstance(appContext).taskDao().insertTask(nextTask);
                            }
                        });
                    }
                }
            }
        });
        snackbar.show();
    }

    private void snoozeTask(Task task, int position) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_snooze, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.snooze_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, w) -> {
                    taskAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(d -> taskAdapter.notifyItemChanged(position))
                .create();

        dialogView.findViewById(R.id.btn_snooze_1hr).setOnClickListener(v -> {
            applySnooze(task, position, System.currentTimeMillis() + (60 * 60 * 1000L));
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btn_snooze_3hr).setOnClickListener(v -> {
            applySnooze(task, position, System.currentTimeMillis() + (3 * 60 * 60 * 1000L));
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btn_snooze_tomorrow).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            applySnooze(task, position, cal.getTimeInMillis());
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btn_snooze_custom).setOnClickListener(v -> {
            dialog.dismiss();
            showCustomSnoozePicker(task, position);
        });

        dialog.show();
    }

    private void showCustomSnoozePicker(Task task, int position) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            new TimePickerDialog(requireContext(), (timeView, hourOfDay, minute) -> {
                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selected.set(Calendar.MINUTE, minute);
                selected.set(Calendar.SECOND, 0);
                selected.set(Calendar.MILLISECOND, 0);
                applySnooze(task, position, selected.getTimeInMillis());
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applySnooze(Task task, int position, long snoozeTime) {
        task.setDueDate(snoozeTime);
        final android.content.Context appContext = requireContext().getApplicationContext();
        try {
            executor.execute(() -> {
                AppDatabase.getInstance(appContext).taskDao().updateTask(task);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        taskAdapter.notifyItemChanged(position);
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                        String msg = getString(R.string.snoozed_until, sdf.format(new Date(snoozeTime)));
                        android.content.Context ctx = getContext();
                        if (ctx != null) {
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor shut down, ignore
        }
    }

    @Override
    public void onTaskStarToggle(Task task, int position) {
        task.setStarred(!task.isStarred());
        final android.content.Context appContext = requireContext().getApplicationContext();
        try {
            executor.execute(() -> {
                AppDatabase.getInstance(appContext).taskDao().updateTask(task);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadTasks());
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor shut down, ignore
        }
    }
}
