package com.taskflow.automate.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks;
    private final OnTaskCompleteListener completeListener;
    private OnTaskClickListener clickListener;
    private final SimpleDateFormat dateFormat;

    public TaskAdapter(List<Task> tasks, OnTaskCompleteListener listener) {
        this.tasks = tasks;
        this.completeListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.clickListener = listener;
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

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityBar = itemView.findViewById(R.id.priority_bar);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textSourceApp = itemView.findViewById(R.id.text_source_app);
            textDueDate = itemView.findViewById(R.id.text_due_date);
            btnMarkComplete = itemView.findViewById(R.id.btn_mark_complete);
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

            btnMarkComplete.setOnClickListener(v -> {
                if (completeListener != null) {
                    completeListener.onTaskComplete(task, position);
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
