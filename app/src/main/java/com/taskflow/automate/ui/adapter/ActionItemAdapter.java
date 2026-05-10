package com.taskflow.automate.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.taskflow.automate.R;
import com.taskflow.automate.util.MeetingTaskExtractor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActionItemAdapter extends RecyclerView.Adapter<ActionItemAdapter.ActionItemViewHolder> {

    private List<MeetingTaskExtractor.ExtractedActionItem> items;
    private final boolean[] selected;
    private final SimpleDateFormat dateFormat;

    public ActionItemAdapter(List<MeetingTaskExtractor.ExtractedActionItem> items) {
        this.items = items;
        this.selected = new boolean[items.size()];
        // All items selected by default
        for (int i = 0; i < selected.length; i++) {
            selected[i] = true;
        }
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ActionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action_item, parent, false);
        return new ActionItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionItemViewHolder holder, int position) {
        MeetingTaskExtractor.ExtractedActionItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<MeetingTaskExtractor.ExtractedActionItem> getSelectedItems() {
        List<MeetingTaskExtractor.ExtractedActionItem> selectedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (selected[i]) {
                selectedItems.add(items.get(i));
            }
        }
        return selectedItems;
    }

    class ActionItemViewHolder extends RecyclerView.ViewHolder {

        private final CheckBox checkboxSelect;
        private final TextView textActionTitle;
        private final Chip chipLanguage;
        private final Chip chipAssignee;
        private final Chip chipDate;

        ActionItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxSelect = itemView.findViewById(R.id.checkbox_select);
            textActionTitle = itemView.findViewById(R.id.text_action_title);
            chipLanguage = itemView.findViewById(R.id.chip_language);
            chipAssignee = itemView.findViewById(R.id.chip_assignee);
            chipDate = itemView.findViewById(R.id.chip_date);
        }

        void bind(MeetingTaskExtractor.ExtractedActionItem item, int position) {
            textActionTitle.setText(item.title);
            checkboxSelect.setChecked(selected[position]);

            checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selected[position] = isChecked;
            });

            // Language chip
            chipLanguage.setText(item.detectedLanguage.name());
            chipLanguage.setVisibility(View.VISIBLE);

            // Assignee chip
            if (item.assigneeName != null && !item.assigneeName.isEmpty()) {
                chipAssignee.setText(item.assigneeName);
                chipAssignee.setVisibility(View.VISIBLE);
            } else {
                chipAssignee.setVisibility(View.GONE);
            }

            // Date chip
            if (item.dueDate != null) {
                chipDate.setText(dateFormat.format(new Date(item.dueDate)));
                chipDate.setVisibility(View.VISIBLE);
            } else {
                chipDate.setVisibility(View.GONE);
            }
        }
    }
}
