package com.taskflow.automate.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.TaskAdapter;
import com.taskflow.automate.util.ReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskCompleteListener {

    private RecyclerView recyclerTasks;
    private TextView textEmptyTasks;
    private FloatingActionButton fabAddTask;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
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
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
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
            List<Task> tasks = AppDatabase.getInstance(requireContext())
                    .taskDao().getAllTasksByPriority();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskList.clear();
                    taskList.addAll(tasks);
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
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().markComplete(task.getId());
            ReminderScheduler.cancelReminder(requireContext(), task.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskAdapter.removeTask(position);
                    updateEmptyState();
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
        final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 9, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedDueDate[0] = selected.getTimeInMillis();
                btnPickDate.setText(dateFormat.format(new Date(selectedDueDate[0])));
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

                    if (!title.isEmpty()) {
                        Task task = new Task();
                        task.setTitle(title);
                        task.setDescription(description);
                        task.setPriority(spinnerPriority.getSelectedItemPosition() + 1);
                        task.setStatus("pending");
                        task.setCreatedAt(System.currentTimeMillis());
                        task.setDueDate(selectedDueDate[0]);

                        executor.execute(() -> {
                            AppDatabase.getInstance(requireContext()).taskDao().insertTask(task);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(this::loadTasks);
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
