package com.taskflow.automate.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.taskflow.automate.util.ReminderScheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodayFragment extends Fragment implements TaskAdapter.OnTaskCompleteListener,
        TaskAdapter.OnTaskClickListener, SwipeCallback.SwipeActionListener {

    private RecyclerView recyclerToday;
    private TextView textEmptyToday;
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

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        taskAdapter.setOnTaskClickListener(this);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadTasks() {
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

            List<Task> todayTasks = AppDatabase.getInstance(requireContext())
                    .taskDao().getTasksDueToday(startOfDay, endOfDay);
            List<Task> overdueTasks = AppDatabase.getInstance(requireContext())
                    .taskDao().getOverdueTasks(now);

            List<Task> combinedTasks = new ArrayList<>();
            combinedTasks.addAll(overdueTasks);
            combinedTasks.addAll(todayTasks);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskList.clear();
                    taskList.addAll(combinedTasks);
                    taskAdapter.updateTasks(taskList);
                    updateEmptyState();
                });
            }
        });
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

        Snackbar snackbar = Snackbar.make(requireView(), R.string.task_completed_message, 5000);
        snackbar.setAction(R.string.undo, v -> {
            taskAdapter.addTask(position, task);
            updateEmptyState();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext())
                                .taskDao().markCompleteWithTimestamp(task.getId(), System.currentTimeMillis());
                        ReminderScheduler.cancelReminder(requireContext(), task.getId());
                    });
                }
            }
        });
        snackbar.show();
    }

    private void snoozeTask(Task task, int position) {
        long snoozeTime = System.currentTimeMillis() + (60 * 60 * 1000L); // 1 hour
        task.setDueDate(snoozeTime);

        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().updateTask(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskAdapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(), R.string.snoozed_message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
