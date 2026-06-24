package com.taskflow.automate.ui;

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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryActionListener {
        void onRestore(Task task, int position);
        void onDelete(Task task, int position);
    }

    private List<Task> tasks;
    private final OnHistoryActionListener listener;
    private final SimpleDateFormat dateFormat;

    public HistoryAdapter(List<Task> tasks, OnHistoryActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
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

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textCompletedDate;
        private final MaterialButton btnRestore;
        private final MaterialButton btnDelete;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_history_title);
            textDescription = itemView.findViewById(R.id.text_history_description);
            textCompletedDate = itemView.findViewById(R.id.text_completed_date);
            btnRestore = itemView.findViewById(R.id.btn_restore);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Task task, int position) {
            textTitle.setText(task.getTitle());

            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(task.getDescription());
            } else {
                textDescription.setVisibility(View.GONE);
            }

            if (task.getCompletedAt() != null) {
                textCompletedDate.setText(String.format("Completed: %s",
                        dateFormat.format(new Date(task.getCompletedAt()))));
            } else {
                textCompletedDate.setText("Completed");
            }

            btnRestore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestore(task, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(task, position);
                }
            });
        }
    }
}
