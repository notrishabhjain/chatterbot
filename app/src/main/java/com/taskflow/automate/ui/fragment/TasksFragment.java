package com.taskflow.automate.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.SwipeCallback;
import com.taskflow.automate.ui.TaskAdapter;
import com.taskflow.automate.ui.TaskEditActivity;
import com.taskflow.automate.util.ReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskCompleteListener,
        TaskAdapter.OnTaskClickListener, SwipeCallback.SwipeActionListener {

    private RecyclerView recyclerTasks;
    private TextView textEmptyTasks;
    private FloatingActionButton fabAddTask;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TextInputEditText searchInput;
    private ChipGroup chipGroupFilter;
    private int selectedPriorityFilter = 0; // 0 = All
    private String currentSearchQuery = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TasksFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerTasks = view.findViewById(R.id.recycler_tasks);
        textEmptyTasks = view.findViewById(R.id.text_empty_tasks);
        fabAddTask = view.findViewById(R.id.fab_add_task);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        taskAdapter.setOnTaskClickListener(this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);

        // Setup swipe gestures
        SwipeCallback swipeCallback = new SwipeCallback(this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerTasks);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        setupSearchAndFilter(view);
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

    private void setupSearchAndFilter(View view) {
        LinearLayout container = view.findViewById(R.id.container_search_filter);

        // Add search input
        searchInput = new TextInputEditText(requireContext());
        searchInput.setHint(R.string.search_tasks_hint);
        searchInput.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, 8);
        searchInput.setLayoutParams(searchParams);
        container.addView(searchInput);

        // Add chip group for filter
        chipGroupFilter = new ChipGroup(requireContext());
        chipGroupFilter.setSingleSelection(true);

        String[] filterLabels = {
                getString(R.string.filter_all),
                getString(R.string.priority_high),
                getString(R.string.priority_medium),
                getString(R.string.priority_low)
        };

        for (int i = 0; i < filterLabels.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(filterLabels[i]);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            if (i == 0) {
                chip.setChecked(true);
            }
            final int priority = i; // 0=All, 1=High, 2=Medium, 3=Low
            chip.setOnClickListener(v -> {
                selectedPriorityFilter = priority;
                loadFilteredTasks();
            });
            chipGroupFilter.addView(chip);
        }

        container.addView(chipGroupFilter);

        // Setup search listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadFilteredTasks();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadTasks() {
        loadFilteredTasks();
    }

    private void loadFilteredTasks() {
        executor.execute(() -> {
            List<Task> tasks;
            if (!currentSearchQuery.isEmpty()) {
                tasks = AppDatabase.getInstance(requireContext())
                        .taskDao().searchTasks(currentSearchQuery);
                // Apply priority filter client-side if both active
                if (selectedPriorityFilter > 0) {
                    List<Task> filtered = new ArrayList<>();
                    for (Task t : tasks) {
                        if (t.getPriority() == selectedPriorityFilter) {
                            filtered.add(t);
                        }
                    }
                    tasks = filtered;
                }
            } else if (selectedPriorityFilter > 0) {
                tasks = AppDatabase.getInstance(requireContext())
                        .taskDao().getTasksByPriority(selectedPriorityFilter);
            } else {
                tasks = AppDatabase.getInstance(requireContext())
                        .taskDao().getAllTasksByPriority();
            }

            final List<Task> result = tasks;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskList.clear();
                    taskList.addAll(result);
                    taskAdapter.updateTasks(taskList);
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            textEmptyTasks.setVisibility(View.VISIBLE);
            recyclerTasks.setVisibility(View.GONE);
        } else {
            textEmptyTasks.setVisibility(View.GONE);
            recyclerTasks.setVisibility(View.VISIBLE);
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
        snackbar.setAnchorView(fabAddTask);
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

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_task, null);

        TextInputEditText editTitle = dialogView.findViewById(R.id.edit_task_title);
        TextInputEditText editDescription = dialogView.findViewById(R.id.edit_task_description);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinner_priority);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btn_pick_date);

        String[] priorities = {
                getString(R.string.priority_high),
                getString(R.string.priority_medium),
                getString(R.string.priority_low)
        };
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default to Medium

        final Long[] selectedDueDate = {null};
        final SimpleDateFormat dateFormatDisplay = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);

                new TimePickerDialog(requireContext(), (timeView, hourOfDay, minute) -> {
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDueDate[0] = selected.getTimeInMillis();
                    btnPickDate.setText(dateFormatDisplay.format(new Date(selectedDueDate[0])));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_task)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String title = editTitle.getText() != null ?
                            editTitle.getText().toString().trim() : "";
                    String description = editDescription.getText() != null ?
                            editDescription.getText().toString().trim() : "";

                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_title_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task task = new Task();
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setPriority(spinnerPriority.getSelectedItemPosition() + 1);
                    task.setStatus("pending");
                    task.setCreatedAt(System.currentTimeMillis());
                    task.setDueDate(selectedDueDate[0]);
                    task.setSourceApp("Manual");

                    executor.execute(() -> {
                        long id = AppDatabase.getInstance(requireContext()).taskDao().insertTask(task);
                        task.setId(id);
                        if (selectedDueDate[0] != null) {
                            ReminderScheduler.scheduleReminder(requireContext(), task);
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(this::loadTasks);
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
