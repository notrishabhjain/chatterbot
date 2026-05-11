package com.taskflow.automate.ui.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.automate.R;
import com.taskflow.automate.model.Subtask;

import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {

    public interface SubtaskActionListener {
        void onSubtaskToggle(Subtask subtask, boolean isChecked);
        void onSubtaskDelete(Subtask subtask, int position);
    }

    private List<Subtask> subtasks;
    private final SubtaskActionListener listener;

    public SubtaskAdapter(List<Subtask> subtasks, SubtaskActionListener listener) {
        this.subtasks = subtasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Subtask subtask = subtasks.get(position);
        holder.bind(subtask, position);
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }

    public void updateSubtasks(List<Subtask> newSubtasks) {
        this.subtasks = newSubtasks;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < subtasks.size()) {
            subtasks.remove(position);
            notifyItemRemoved(position);
        }
    }

    class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkboxSubtask;
        private final TextView textSubtaskTitle;
        private final ImageButton btnDeleteSubtask;

        SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxSubtask = itemView.findViewById(R.id.checkbox_subtask);
            textSubtaskTitle = itemView.findViewById(R.id.text_subtask_title);
            btnDeleteSubtask = itemView.findViewById(R.id.btn_delete_subtask);
        }

        void bind(Subtask subtask, int position) {
            textSubtaskTitle.setText(subtask.getTitle());
            checkboxSubtask.setOnCheckedChangeListener(null);
            checkboxSubtask.setChecked(subtask.isCompleted());

            // Strikethrough for completed
            if (subtask.isCompleted()) {
                textSubtaskTitle.setPaintFlags(textSubtaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textSubtaskTitle.setPaintFlags(textSubtaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

            checkboxSubtask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSubtaskToggle(subtask, isChecked);
                }
            });

            btnDeleteSubtask.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubtaskDelete(subtask, position);
                }
            });
        }
    }
}
