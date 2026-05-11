package com.taskflow.automate.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Tag;
import com.taskflow.automate.model.Task;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskCompleteListener {
        void onTaskComplete(Task task, int position);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskStarListener {
        void onTaskStarToggle(Task task, int position);
    }

    private List<Task> tasks;
    private final OnTaskCompleteListener completeListener;
    private OnTaskClickListener clickListener;
    private OnTaskStarListener starListener;
    private final SimpleDateFormat dateFormat;
    private Map<Long, List<Tag>> tagMap = new HashMap<>();

    public TaskAdapter(List<Task> tasks, OnTaskCompleteListener listener) {
        this.tasks = tasks;
        this.completeListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnTaskStarListener(OnTaskStarListener listener) {
        this.starListener = listener;
    }

    public void setTagMap(Map<Long, List<Tag>> tagMap) {
        this.tagMap = tagMap;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, position);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    public void removeTask(int position) {
        if (position >= 0 && position < tasks.size()) {
            tasks.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tasks.size());
        }
    }

    public void addTask(int position, Task task) {
        if (position >= 0 && position <= tasks.size()) {
            tasks.add(position, task);
            notifyItemInserted(position);
            notifyItemRangeChanged(position, tasks.size());
        }
    }

    public Task getTaskAtPosition(int position) {
        if (position >= 0 && position < tasks.size()) {
            return tasks.get(position);
        }
        return null;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final View priorityBar;
        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textSourceApp;
        private final TextView textDueDate;
        private final MaterialButton btnMarkComplete;
        private final ImageButton btnAddCalendar;
        private final ImageButton btnStar;
        private final ChipGroup chipGroupTaskTags;
        private final TextView textSubtaskProgress;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityBar = itemView.findViewById(R.id.priority_bar);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textSourceApp = itemView.findViewById(R.id.text_source_app);
            textDueDate = itemView.findViewById(R.id.text_due_date);
            btnMarkComplete = itemView.findViewById(R.id.btn_mark_complete);
            btnAddCalendar = itemView.findViewById(R.id.btn_add_calendar);
            btnStar = itemView.findViewById(R.id.btn_star);
            chipGroupTaskTags = itemView.findViewById(R.id.chip_group_task_tags);
            textSubtaskProgress = itemView.findViewById(R.id.text_subtask_progress);
        }

        void bind(Task task, int position) {
            textTitle.setText(task.getTitle());
            textDescription.setText(task.getDescription());

            if (task.getSourceApp() != null && !task.getSourceApp().isEmpty()) {
                textSourceApp.setVisibility(View.VISIBLE);
                textSourceApp.setText(String.format("From: %s", task.getSourceApp()));
            } else {
                textSourceApp.setVisibility(View.GONE);
            }

            if (task.getDueDate() != null) {
                textDueDate.setVisibility(View.VISIBLE);
                textDueDate.setText(String.format("Due: %s", dateFormat.format(new Date(task.getDueDate()))));
            } else {
                textDueDate.setVisibility(View.GONE);
            }

            // Set priority color bar
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
            priorityBar.setBackgroundColor(priorityColor);

            // Show tags
            chipGroupTaskTags.removeAllViews();
            List<Tag> taskTags = tagMap.get(task.getId());
            if (taskTags == null) taskTags = Collections.emptyList();
            if (!taskTags.isEmpty()) {
                chipGroupTaskTags.setVisibility(View.VISIBLE);
                for (Tag tag : taskTags) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(tag.getName());
                    chip.setTextSize(10);
                    chip.setClickable(false);
                    chip.setChipMinHeight(24f);
                    if (tag.getColor() != null && !tag.getColor().isEmpty()) {
                        try {
                            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(tag.getColor())));
                            chip.setTextColor(Color.WHITE);
                        } catch (IllegalArgumentException e) {
                            // ignore invalid colors
                        }
                    }
                    chipGroupTaskTags.addView(chip);
                }
            } else {
                chipGroupTaskTags.setVisibility(View.GONE);
            }

            btnMarkComplete.setOnClickListener(v -> {
                if (completeListener != null) {
                    completeListener.onTaskComplete(task, position);
                }
            });

            // Star button
            btnStar.setImageResource(task.isStarred() ?
                    android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            btnStar.setOnClickListener(v -> {
                if (starListener != null) {
                    starListener.onTaskStarToggle(task, position);
                }
            });

            // Subtask progress
            Context ctx = itemView.getContext();
            new Thread(() -> {
                int total = AppDatabase.getInstance(ctx).subtaskDao().getTotalSubtaskCount(task.getId());
                int completed = AppDatabase.getInstance(ctx).subtaskDao().getCompletedSubtaskCount(task.getId());
                if (total > 0) {
                    String progress = completed + "/" + total + " done";
                    textSubtaskProgress.post(() -> {
                        textSubtaskProgress.setText(progress);
                        textSubtaskProgress.setVisibility(View.VISIBLE);
                    });
                } else {
                    textSubtaskProgress.post(() -> textSubtaskProgress.setVisibility(View.GONE));
                }
            }).start();

            btnAddCalendar.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setData(CalendarContract.Events.CONTENT_URI);

                // Use description as event title if available (notification title is often sender name)
                String eventTitle;
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    eventTitle = task.getDescription();
                } else {
                    eventTitle = task.getTitle();
                }
                // Truncate if too long for calendar title
                if (eventTitle.length() > 100) {
                    eventTitle = eventTitle.substring(0, 97) + "...";
                }

                intent.putExtra(CalendarContract.Events.TITLE, eventTitle);

                // Put the full context in description
                String eventDescription = "Task: " + task.getTitle();
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    eventDescription += "\n\n" + task.getDescription();
                }
                if (task.getSourceApp() != null && !task.getSourceApp().isEmpty()) {
                    eventDescription += "\n\nSource: " + task.getSourceApp();
                }
                eventDescription += "\nPriority: " + task.getPriorityLabel();
                intent.putExtra(CalendarContract.Events.DESCRIPTION, eventDescription);

                long startTime = task.getDueDate() != null ? task.getDueDate() : System.currentTimeMillis();
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTime + 60 * 60 * 1000);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTaskClick(task);
                }
            });
        }
    }
}
