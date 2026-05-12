package com.taskflow.automate.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Subtask;
import com.taskflow.automate.model.Tag;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.model.TaskTagCrossRef;
import com.taskflow.automate.ui.adapter.SubtaskAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskEditActivity extends AppCompatActivity implements SubtaskAdapter.SubtaskActionListener {

    public static final String EXTRA_TASK_ID = "extra_task_id";

    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private Spinner spinnerPriority;
    private Spinner spinnerRecurrence;
    private MaterialButton btnPickDueDate;
    private MaterialButton btnSave;
    private ChipGroup chipGroupTags;
    private Chip chipAddTag;
    private RecyclerView recyclerSubtasks;
    private SubtaskAdapter subtaskAdapter;
    private List<Subtask> subtaskList = new ArrayList<>();
    private MaterialButton btnAddSubtask;

    private Task currentTask;
    private Long selectedDueDate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    private static final String[] RECURRENCE_OPTIONS = {"None", "Daily", "Weekly", "Monthly"};
    private static final String[] RECURRENCE_VALUES = {null, "DAILY", "WEEKLY", "MONTHLY"};

    private static final String[] TAG_COLORS = {"#F44336", "#2196F3", "#4CAF50", "#9C27B0", "#FF9800", "#009688"};
    private static final String[] TAG_COLOR_NAMES = {"Red", "Blue", "Green", "Purple", "Orange", "Teal"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_edit);

        setupToolbar();
        initViews();
        setupPrioritySpinner();
        setupRecurrenceSpinner();
        setupDatePicker();
        setupSaveButton();
        setupTagAdd();

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);

        // If recreated from saved state but taskId is invalid, finish gracefully
        if (taskId == -1 && savedInstanceState != null) {
            finish();
            return;
        }

        if (taskId != -1) {
            loadTask(taskId);
        } else {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Do NOT shut down executor here - pending async operations (save, load)
        // may still need it to complete. The executor will be GC'd naturally.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedDueDate != null) {
            outState.putLong("saved_due_date", selectedDueDate);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            long savedDueDate = savedInstanceState.getLong("saved_due_date", -1);
            if (savedDueDate != -1) {
                selectedDueDate = savedDueDate;
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_edit);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        spinnerPriority = findViewById(R.id.spinner_priority);
        spinnerRecurrence = findViewById(R.id.spinner_recurrence);
        btnPickDueDate = findViewById(R.id.btn_pick_due_date);
        btnSave = findViewById(R.id.btn_save);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        chipAddTag = findViewById(R.id.chip_add_tag);
        recyclerSubtasks = findViewById(R.id.recycler_subtasks);
        btnAddSubtask = findViewById(R.id.btn_add_subtask);

        recyclerSubtasks.setLayoutManager(new LinearLayoutManager(this));
        subtaskAdapter = new SubtaskAdapter(subtaskList, this);
        recyclerSubtasks.setAdapter(subtaskAdapter);

        btnAddSubtask.setOnClickListener(v -> showAddSubtaskDialog());
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

    private void setupRecurrenceSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, RECURRENCE_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrence.setAdapter(adapter);
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

    private void setupTagAdd() {
        chipAddTag.setOnClickListener(v -> showAddTagDialog());
    }

    private void loadTask(long taskId) {
        try {
            executor.execute(() -> {
                try {
                    currentTask = AppDatabase.getInstance(this).taskDao().getTaskById(taskId);
                    if (currentTask != null) {
                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                populateFields();
                            }
                        });
                        loadTagsForTask(taskId);
                        loadSubtasks(taskId);
                    } else {
                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTagsForTask(long taskId) {
        try {
            executor.execute(() -> {
                try {
                    List<Tag> tags = AppDatabase.getInstance(this).tagDao().getTagsForTask(taskId);
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            chipGroupTags.removeAllViews();
                            for (Tag tag : tags) {
                                addTagChip(tag);
                            }
                        }
                    });
                } catch (Exception e) {
                    // Prevent crash from DB or UI thread issues
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
    }

    private void addTagChip(Tag tag) {
        Chip chip = new Chip(this);
        chip.setText(tag.getName());
        chip.setCloseIconVisible(true);
        if (tag.getColor() != null && !tag.getColor().isEmpty()) {
            try {
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setChipStrokeColorResource(android.R.color.transparent);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor(tag.getColor())));
                chip.setTextColor(Color.WHITE);
                chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(Color.WHITE));
            } catch (IllegalArgumentException e) {
                // ignore invalid colors
            }
        }
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            if (currentTask == null) return;
            try {
                executor.execute(() -> {
                    try {
                        TaskTagCrossRef crossRef = new TaskTagCrossRef();
                        crossRef.setTaskId(currentTask.getId());
                        crossRef.setTagId(tag.getId());
                        AppDatabase.getInstance(this).tagDao().deleteTaskTagCrossRef(crossRef);
                    } catch (Exception e) {
                        // Prevent crash from executor shutdown or DB error
                    }
                });
            } catch (Exception e) {
                // Executor may be shut down
            }
        });
        chipGroupTags.addView(chip);
    }

    private void showAddTagDialog() {
        try {
            executor.execute(() -> {
                try {
                    List<Tag> allTags = AppDatabase.getInstance(this).tagDao().getAllTags();
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            if (allTags.isEmpty()) {
                                showCreateTagDialog();
                            } else {
                                showSelectTagDialog(allTags);
                            }
                        }
                    });
                } catch (Exception e) {
                    // Prevent crash from DB or UI thread issues
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
    }

    private void showSelectTagDialog(List<Tag> allTags) {
        String[] tagNames = new String[allTags.size() + 1];
        for (int i = 0; i < allTags.size(); i++) {
            tagNames[i] = allTags.get(i).getName();
        }
        tagNames[allTags.size()] = "+ " + getString(R.string.create_new_tag);

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_tag)
                .setItems(tagNames, (dialog, which) -> {
                    if (which == allTags.size()) {
                        showCreateTagDialog();
                    } else {
                        Tag selectedTag = allTags.get(which);
                        assignTagToTask(selectedTag);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void assignTagToTask(Tag tag) {
        if (currentTask == null) return;
        try {
            executor.execute(() -> {
                try {
                    TaskTagCrossRef crossRef = new TaskTagCrossRef();
                    crossRef.setTaskId(currentTask.getId());
                    crossRef.setTagId(tag.getId());
                    AppDatabase.getInstance(this).tagDao().insertTaskTagCrossRef(crossRef);
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            addTagChip(tag);
                        }
                    });
                } catch (Exception e) {
                    // Prevent crash from DB or UI thread issues
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
    }

    private void showCreateTagDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null);
        TextInputEditText editTagName = dialogView.findViewById(R.id.edit_tag_name);
        ChipGroup chipGroupColors = dialogView.findViewById(R.id.chip_group_colors);

        final String[] selectedColor = {TAG_COLORS[0]};

        for (int i = 0; i < TAG_COLORS.length; i++) {
            Chip colorChip = new Chip(this);
            colorChip.setText(TAG_COLOR_NAMES[i]);
            colorChip.setCheckable(true);
            if (i == 0) colorChip.setChecked(true);
            try {
                colorChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor(TAG_COLORS[i])));
                colorChip.setTextColor(Color.WHITE);
            } catch (IllegalArgumentException e) {
                // ignore
            }
            final int index = i;
            colorChip.setOnClickListener(cv -> {
                selectedColor[0] = TAG_COLORS[index];
                // Uncheck others
                for (int j = 0; j < chipGroupColors.getChildCount(); j++) {
                    ((Chip) chipGroupColors.getChildAt(j)).setChecked(j == index);
                }
            });
            chipGroupColors.addView(colorChip);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_new_tag)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = editTagName.getText() != null ?
                            editTagName.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        Tag newTag = new Tag();
                        newTag.setName(name);
                        newTag.setColor(selectedColor[0]);
                        try {
                            executor.execute(() -> {
                                try {
                                    long tagId = AppDatabase.getInstance(this).tagDao().insertTag(newTag);
                                    newTag.setId(tagId);
                                    if (currentTask != null) {
                                        assignTagToTask(newTag);
                                    }
                                } catch (Exception e) {
                                    // Prevent crash from DB error
                                }
                            });
                        } catch (Exception e) {
                            // Executor may be shut down
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void populateFields() {
        if (currentTask == null) return;
        try {
            editTitle.setText(currentTask.getTitle() != null ? currentTask.getTitle() : "");
            editDescription.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "");

            // Clamp priority to valid spinner range [0, 2]
            int priorityIndex = currentTask.getPriority() - 1;
            spinnerPriority.setSelection(Math.max(0, Math.min(2, priorityIndex)));

            if (currentTask.getDueDate() != null) {
                selectedDueDate = currentTask.getDueDate();
                btnPickDueDate.setText(dateFormat.format(new Date(selectedDueDate)));
            }

            // Set recurrence spinner
            String rule = currentTask.getRecurrenceRule();
            if (rule != null) {
                for (int i = 0; i < RECURRENCE_VALUES.length; i++) {
                    if (rule.equals(RECURRENCE_VALUES[i])) {
                        spinnerRecurrence.setSelection(i);
                        break;
                    }
                }
            } else {
                spinnerRecurrence.setSelection(0);
            }
        } catch (Exception e) {
            // Prevent crash from unexpected data - fields will remain at defaults
        }
    }

    private void loadSubtasks(long taskId) {
        try {
            executor.execute(() -> {
                try {
                    List<Subtask> subtasks = AppDatabase.getInstance(this).subtaskDao().getSubtasksForTask(taskId);
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            subtaskList.clear();
                            subtaskList.addAll(subtasks);
                            subtaskAdapter.updateSubtasks(subtaskList);
                        }
                    });
                } catch (Exception e) {
                    // Prevent crash from DB or UI thread issues
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
    }

    private void showAddSubtaskDialog() {
        if (currentTask == null) return;

        EditText editText = new EditText(this);
        editText.setHint(R.string.enter_subtask_title);
        editText.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_subtask)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty() && currentTask != null) {
                        Subtask subtask = new Subtask();
                        subtask.setTaskId(currentTask.getId());
                        subtask.setTitle(title);
                        subtask.setCompleted(false);
                        subtask.setCreatedAt(System.currentTimeMillis());
                        try {
                            executor.execute(() -> {
                                try {
                                    long id = AppDatabase.getInstance(this).subtaskDao().insertSubtask(subtask);
                                    subtask.setId(id);
                                    runOnUiThread(() -> {
                                        if (!isFinishing()) {
                                            subtaskList.add(subtask);
                                            subtaskAdapter.notifyItemInserted(subtaskList.size() - 1);
                                        }
                                    });
                                } catch (Exception e) {
                                    // Prevent crash from DB or UI thread issues
                                }
                            });
                        } catch (Exception e) {
                            // Executor may be shut down
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSubtaskToggle(Subtask subtask, boolean isChecked) {
        if (currentTask == null || subtask == null) return;
        subtask.setCompleted(isChecked);
        try {
            executor.execute(() -> {
                try {
                    AppDatabase.getInstance(this).subtaskDao().updateSubtask(subtask);
                } catch (Exception e) {
                    // Prevent crash from DB error
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
        // Refresh to update strikethrough
        int index = subtaskList.indexOf(subtask);
        if (index >= 0) {
            subtaskAdapter.notifyItemChanged(index);
        }
    }

    @Override
    public void onSubtaskDelete(Subtask subtask, int position) {
        if (currentTask == null || subtask == null) return;
        try {
            executor.execute(() -> {
                try {
                    AppDatabase.getInstance(this).subtaskDao().deleteSubtask(subtask);
                } catch (Exception e) {
                    // Prevent crash from DB error
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
        }
        subtaskAdapter.removeItem(position);
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

        // Set recurrence
        int recurrenceIndex = spinnerRecurrence.getSelectedItemPosition();
        currentTask.setRecurrenceRule(RECURRENCE_VALUES[recurrenceIndex]);
        if (recurrenceIndex > 0) {
            currentTask.setRecurrenceInterval(1);
        } else {
            currentTask.setRecurrenceInterval(0);
        }

        try {
            executor.execute(() -> {
                try {
                    AppDatabase.getInstance(this).taskDao().updateTask(currentTask);
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            // Executor may be shut down
            finish();
        }
    }
}
