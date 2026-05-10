package com.taskflow.automate.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.taskflow.automate.R;
import com.taskflow.automate.model.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskCompleteListener {
        void onTaskComplete(Task task, int position);
    }

    private List<Task> tasks;
    private final OnTaskCompleteListener listener;
    private final SimpleDateFormat dateFormat;

    public TaskAdapter(List<Task> tasks, OnTaskCompleteListener listener) {
        this.tasks = tasks;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
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

    private int getTaskTypeColor(String taskType) {
        if (taskType == null) {
            return Color.parseColor("#616161");
        }
        switch (taskType.toUpperCase(Locale.US)) {
            case "MEETING":
                return Color.parseColor("#1565C0");
            case "DEADLINE":
                return Color.parseColor("#C62828");
            case "FOLLOW_UP":
                return Color.parseColor("#E65100");
            case "REQUEST":
                return Color.parseColor("#2E7D32");
            case "APPROVAL":
                return Color.parseColor("#6A1B9A");
            case "REMINDER":
                return Color.parseColor("#00838F");
            case "GENERAL":
            default:
                return Color.parseColor("#616161");
        }
    }

    private String getTaskTypeLabel(String taskType) {
        if (taskType == null) {
            return "GENERAL";
        }
        switch (taskType.toUpperCase(Locale.US)) {
            case "MEETING":
                return "MEETING";
            case "DEADLINE":
                return "DEADLINE";
            case "FOLLOW_UP":
                return "FOLLOW UP";
            case "REQUEST":
                return "REQUEST";
            case "APPROVAL":
                return "APPROVAL";
            case "REMINDER":
                return "REMINDER";
            case "GENERAL":
            default:
                return "GENERAL";
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final View priorityBar;
        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textSourceApp;
        private final TextView textDueDate;
        private final TextView textTaskTypeBadge;
        private final TextView textAssigner;
        private final TextView textFollowUpIndicator;
        private final MaterialButton btnMarkComplete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityBar = itemView.findViewById(R.id.priority_bar);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textSourceApp = itemView.findViewById(R.id.text_source_app);
            textDueDate = itemView.findViewById(R.id.text_due_date);
            textTaskTypeBadge = itemView.findViewById(R.id.text_task_type_badge);
            textAssigner = itemView.findViewById(R.id.text_assigner);
            textFollowUpIndicator = itemView.findViewById(R.id.text_follow_up_indicator);
            btnMarkComplete = itemView.findViewById(R.id.btn_mark_complete);
        }

        void bind(Task task, int position) {
            textTitle.setText(task.getTitle());
            textDescription.setText(task.getDescription());

            // Source app
            if (task.getSourceApp() != null && !task.getSourceApp().isEmpty()) {
                textSourceApp.setVisibility(View.VISIBLE);
                textSourceApp.setText(String.format("From: %s", task.getSourceApp()));
            } else {
                textSourceApp.setVisibility(View.GONE);
            }

            // Due date with overdue coloring
            if (task.getDueDate() != null) {
                textDueDate.setVisibility(View.VISIBLE);
                textDueDate.setText(String.format("Due: %s", dateFormat.format(new Date(task.getDueDate()))));
                if (task.getDueDate() < System.currentTimeMillis()) {
                    textDueDate.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    textDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textSecondary));
                }
            } else {
                textDueDate.setVisibility(View.GONE);
            }

            // Task type badge
            String taskType = task.getTaskType();
            if (taskType != null && !taskType.isEmpty()) {
                textTaskTypeBadge.setVisibility(View.VISIBLE);
                textTaskTypeBadge.setText(getTaskTypeLabel(taskType));
                int typeColor = getTaskTypeColor(taskType);
                GradientDrawable background = (GradientDrawable) textTaskTypeBadge.getBackground().mutate();
                background.setColor(typeColor);
                textTaskTypeBadge.setBackground(background);
            } else {
                textTaskTypeBadge.setVisibility(View.GONE);
            }

            // Assigner
            if (task.getAssigner() != null && !task.getAssigner().isEmpty()) {
                textAssigner.setVisibility(View.VISIBLE);
                textAssigner.setText(String.format("Assigned by: %s", task.getAssigner()));
            } else {
                textAssigner.setVisibility(View.GONE);
            }

            // Follow-up indicator
            if (task.isFollowUp()) {
                textFollowUpIndicator.setVisibility(View.VISIBLE);
                GradientDrawable followUpBg = (GradientDrawable) textFollowUpIndicator.getBackground().mutate();
                followUpBg.setColor(Color.parseColor("#E65100"));
                textFollowUpIndicator.setBackground(followUpBg);
            } else {
                textFollowUpIndicator.setVisibility(View.GONE);
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

            btnMarkComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskComplete(task, position);
                }
            });
        }
    }
}
