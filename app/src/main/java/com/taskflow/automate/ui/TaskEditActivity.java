package com.taskflow.automate.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskEditActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private Spinner spinnerPriority;
    private MaterialButton btnPickDueDate;
    private MaterialButton btnSave;

    private Task currentTask;
    private Long selectedDueDate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_edit);

        setupToolbar();
        initViews();
        setupPrioritySpinner();
        setupDatePicker();
        setupSaveButton();

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            loadTask(taskId);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_edit);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        spinnerPriority = findViewById(R.id.spinner_priority);
        btnPickDueDate = findViewById(R.id.btn_pick_due_date);
        btnSave = findViewById(R.id.btn_save);
    }

    private void setupPrioritySpinner() {
        String[] priorities = {
                getString(R.string.priority_high),
                getString(R.string.priority_medium),
                getString(R.string.priority_low)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);
    }

    private void setupDatePicker() {
        btnPickDueDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (selectedDueDate != null) {
                calendar.setTimeInMillis(selectedDueDate);
            }
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                if (selectedDueDate != null) {
                    selected.setTimeInMillis(selectedDueDate);
                }
                selected.set(Calendar.YEAR, year);
                selected.set(Calendar.MONTH, month);
                selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDueDate = selected.getTimeInMillis();
                    btnPickDueDate.setText(dateFormat.format(new Date(selectedDueDate)));
                }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), false).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveTask());
    }

    private void loadTask(long taskId) {
        executor.execute(() -> {
            currentTask = AppDatabase.getInstance(this).taskDao().getTaskById(taskId);
            if (currentTask != null) {
                runOnUiThread(() -> populateFields());
            } else {
                runOnUiThread(this::finish);
            }
        });
    }

    private void populateFields() {
        editTitle.setText(currentTask.getTitle());
        editDescription.setText(currentTask.getDescription());
        spinnerPriority.setSelection(currentTask.getPriority() - 1);

        if (currentTask.getDueDate() != null) {
            selectedDueDate = currentTask.getDueDate();
            btnPickDueDate.setText(dateFormat.format(new Date(selectedDueDate)));
        }
    }

    private void saveTask() {
        String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
        String description = editDescription.getText() != null ?
                editDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            editTitle.setError(getString(R.string.error_title_required));
            return;
        }

        if (currentTask == null) {
            return;
        }

        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setPriority(spinnerPriority.getSelectedItemPosition() + 1);
        currentTask.setDueDate(selectedDueDate);

        executor.execute(() -> {
            AppDatabase.getInstance(this).taskDao().updateTask(currentTask);
            runOnUiThread(() -> {
                setResult(RESULT_OK);
                finish();
            });
        });
    }
}
