package com.taskflow.automate.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
import com.taskflow.automate.util.BadgeUtils;
import com.taskflow.automate.util.RecurringTaskManager;
import com.taskflow.automate.util.ReminderScheduler;
import com.taskflow.automate.widget.TaskWidgetProvider;

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

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskCompleteListener,
        TaskAdapter.OnTaskClickListener, SwipeCallback.SwipeActionListener,
        TaskAdapter.OnTaskStarListener, TaskAdapter.OnTaskLongClickListener {

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

    // Bulk selection mode
    private boolean isSelectionMode = false;
    private LinearLayout bulkActionBar;
    private TextView textSelectedCount;

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
        taskAdapter.setOnTaskStarListener(this);
        taskAdapter.setOnTaskLongClickListener(this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);

        // Setup swipe gestures
        SwipeCallback swipeCallback = new SwipeCallback(this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerTasks);

        fabAddTask.setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
            } else {
                showAddTaskDialog();
            }
        });

        setupSearchAndFilter(view);
        setupBulkActionBar(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    private void setupBulkActionBar(View view) {
        // Find the inner LinearLayout container within the CoordinatorLayout
        ViewGroup coordinatorLayout = (ViewGroup) view;
        LinearLayout innerLayout = null;
        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
            View child = coordinatorLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                innerLayout = (LinearLayout) child;
                break;
            }
        }
        if (innerLayout == null) {
            return;
        }

        bulkActionBar = new LinearLayout(requireContext());
        bulkActionBar.setOrientation(LinearLayout.VERTICAL);
        bulkActionBar.setBackgroundColor(0xFFF5F5F5);
        bulkActionBar.setPadding(16, 12, 16, 12);
        bulkActionBar.setVisibility(View.GONE);
        bulkActionBar.setElevation(8f);

        // Selected count text
        textSelectedCount = new TextView(requireContext());
        textSelectedCount.setTextSize(14);
        textSelectedCount.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countParams.setMargins(0, 0, 0, 8);
        textSelectedCount.setLayoutParams(countParams);
        bulkActionBar.addView(textSelectedCount);

        // Button row
        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonRow.setLayoutParams(rowParams);

        // Complete All button
        MaterialButton btnCompleteAll = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCompleteAll.setText(R.string.bulk_complete_all);
        btnCompleteAll.setTextSize(12);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(4, 0, 4, 0);
        btnCompleteAll.setLayoutParams(btnParams);
        btnCompleteAll.setOnClickListener(v -> bulkCompleteSelected());
        buttonRow.addView(btnCompleteAll);

        // Delete All button
        MaterialButton btnDeleteAll = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDeleteAll.setText(R.string.bulk_delete_all);
        btnDeleteAll.setTextSize(12);
        LinearLayout.LayoutParams btnDeleteParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnDeleteParams.setMargins(4, 0, 4, 0);
        btnDeleteAll.setLayoutParams(btnDeleteParams);
        btnDeleteAll.setOnClickListener(v -> bulkDeleteSelected());
        buttonRow.addView(btnDeleteAll);

        // Change Priority button
        MaterialButton btnChangePriority = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnChangePriority.setText(R.string.bulk_change_priority);
        btnChangePriority.setTextSize(12);
        LinearLayout.LayoutParams btnPriorityParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnPriorityParams.setMargins(4, 0, 4, 0);
        btnChangePriority.setLayoutParams(btnPriorityParams);
        btnChangePriority.setOnClickListener(v -> bulkChangePriority());
        buttonRow.addView(btnChangePriority);

        bulkActionBar.addView(buttonRow);

        // Select All / Deselect All row
        LinearLayout selectRow = new LinearLayout(requireContext());
        selectRow.setOrientation(LinearLayout.HORIZONTAL);
        selectRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams selectRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        selectRowParams.setMargins(0, 8, 0, 0);
        selectRow.setLayoutParams(selectRowParams);

        MaterialButton btnSelectAll = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonTextStyle);
        btnSelectAll.setText("Select All");
        btnSelectAll.setTextSize(12);
        btnSelectAll.setOnClickListener(v -> {
            taskAdapter.selectAll();
            updateSelectedCount();
        });
        selectRow.addView(btnSelectAll);

        MaterialButton btnDeselectAll = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonTextStyle);
        btnDeselectAll.setText("Deselect All");
        btnDeselectAll.setTextSize(12);
        btnDeselectAll.setOnClickListener(v -> {
            taskAdapter.deselectAll();
            updateSelectedCount();
        });
        selectRow.addView(btnDeselectAll);

        bulkActionBar.addView(selectRow);

        // Add the bar at the end of the inner LinearLayout
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        innerLayout.addView(bulkActionBar, barParams);
    }

    private void enterSelectionMode(int initialPosition) {
        isSelectionMode = true;
        taskAdapter.setSelectionMode(true);
        taskAdapter.getSelectedPositions().add(initialPosition);
        taskAdapter.notifyItemChanged(initialPosition);
        bulkActionBar.setVisibility(View.VISIBLE);
        fabAddTask.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        updateSelectedCount();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        taskAdapter.setSelectionMode(false);
        bulkActionBar.setVisibility(View.GONE);
        fabAddTask.setImageResource(android.R.drawable.ic_input_add);
    }

    private void updateSelectedCount() {
        int count = taskAdapter.getSelectedCount();
        textSelectedCount.setText(getString(R.string.bulk_selected_count, count));
    }

    @Override
    public void onTaskLongClick(Task task, int position) {
        if (!isSelectionMode) {
            enterSelectionMode(position);
        }
    }

    private void bulkCompleteSelected() {
        List<Task> selected = taskAdapter.getSelectedTasks();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_items_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        final int count = selected.size();
        executor.execute(() -> {
            for (Task task : selected) {
                AppDatabase.getInstance(requireContext())
                        .taskDao().markCompleteWithTimestamp(task.getId(), System.currentTimeMillis());
                ReminderScheduler.cancelReminder(requireContext(), task.getId());
            }
            BadgeUtils.updateBadgeCount(requireContext());
            TaskWidgetProvider.refreshWidget(requireContext());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    exitSelectionMode();
                    loadTasks();
                    Snackbar.make(requireView(),
                            getString(R.string.bulk_complete_confirm, count),
                            Snackbar.LENGTH_LONG).setAnchorView(fabAddTask).show();
                });
            }
        });
    }

    private void bulkDeleteSelected() {
        List<Task> selected = taskAdapter.getSelectedTasks();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_items_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.bulk_delete_confirm, selected.size()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    executor.execute(() -> {
                        for (Task task : selected) {
                            AppDatabase.getInstance(requireContext())
                                    .taskDao().deleteTask(task.getId());
                            ReminderScheduler.cancelReminder(requireContext(), task.getId());
                        }
                        BadgeUtils.updateBadgeCount(requireContext());
                        TaskWidgetProvider.refreshWidget(requireContext());

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                exitSelectionMode();
                                loadTasks();
                            });
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void bulkChangePriority() {
        List<Task> selected = taskAdapter.getSelectedTasks();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_items_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] priorityOptions = {
                getString(R.string.priority_high),
                getString(R.string.priority_medium),
                getString(R.string.priority_low)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.bulk_change_priority)
                .setItems(priorityOptions, (dialog, which) -> {
                    int newPriority = which + 1; // 1=High, 2=Medium, 3=Low
                    executor.execute(() -> {
                        for (Task task : selected) {
                            task.setPriority(newPriority);
                            AppDatabase.getInstance(requireContext())
                                    .taskDao().updateTask(task);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                exitSelectionMode();
                                loadTasks();
                            });
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
                        .taskDao().getAllTasksByPriorityWithStarred();
            }

            // Pre-load subtask counts
            Map<Long, int[]> countMap = new HashMap<>();
            for (Task t : tasks) {
                int total = AppDatabase.getInstance(requireContext()).subtaskDao().getTotalSubtaskCount(t.getId());
                if (total > 0) {
                    int completed = AppDatabase.getInstance(requireContext()).subtaskDao().getCompletedSubtaskCount(t.getId());
                    countMap.put(t.getId(), new int[]{completed, total});
                }
            }

            final List<Task> result = tasks;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskList.clear();
                    taskList.addAll(result);
                    taskAdapter.setSubtaskCountMap(countMap);
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
                        BadgeUtils.updateBadgeCount(requireContext());

                        // Handle recurring tasks
                        RecurringTaskManager recurringManager = new RecurringTaskManager();
                        Task nextTask = recurringManager.createNextRecurrence(task);
                        if (nextTask != null) {
                            AppDatabase.getInstance(requireContext()).taskDao().insertTask(nextTask);
                        }

                        // Refresh widget to reflect completed task
                        TaskWidgetProvider.refreshWidget(requireContext());
                    });
                }
            }
        });
        snackbar.setAnchorView(fabAddTask);
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
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().updateTask(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskAdapter.notifyItemChanged(position);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    String msg = getString(R.string.snoozed_until, sdf.format(new Date(snoozeTime)));
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onTaskStarToggle(Task task, int position) {
        task.setStarred(!task.isStarred());
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().updateTask(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> loadTasks());
            }
        });
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_task, null);

        TextInputEditText editTitle = dialogView.findViewById(R.id.edit_task_title);
        TextInputEditText editDescription = dialogView.findViewById(R.id.edit_task_description);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinner_priority);
        Spinner spinnerRecurrence = dialogView.findViewById(R.id.spinner_recurrence);
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

        String[] recurrenceOptions = {
                getString(R.string.recurrence_none),
                getString(R.string.recurrence_daily),
                getString(R.string.recurrence_weekly),
                getString(R.string.recurrence_monthly)
        };
        ArrayAdapter<String> recurrenceAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, recurrenceOptions);
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrence.setAdapter(recurrenceAdapter);

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

                    // Set recurrence
                    int recurrenceIndex = spinnerRecurrence.getSelectedItemPosition();
                    String[] recurrenceValues = {null, "DAILY", "WEEKLY", "MONTHLY"};
                    task.setRecurrenceRule(recurrenceValues[recurrenceIndex]);
                    if (recurrenceIndex > 0) {
                        task.setRecurrenceInterval(1);
                    }

                    executor.execute(() -> {
                        long id = AppDatabase.getInstance(requireContext()).taskDao().insertTask(task);
                        task.setId(id);
                        if (selectedDueDate[0] != null) {
                            ReminderScheduler.scheduleReminder(requireContext(), task);
                        }
                        // Refresh widget to show newly created task
                        TaskWidgetProvider.refreshWidget(requireContext());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(this::loadTasks);
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
