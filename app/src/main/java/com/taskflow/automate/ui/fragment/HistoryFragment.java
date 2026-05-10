package com.taskflow.automate.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.HistoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnHistoryActionListener {

    private RecyclerView recyclerHistory;
    private TextView textEmptyHistory;
    private HistoryAdapter historyAdapter;
    private List<Task> taskList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerHistory = view.findViewById(R.id.recycler_history);
        textEmptyHistory = view.findViewById(R.id.text_empty_history);

        taskList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(taskList, this);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerHistory.setAdapter(historyAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCompletedTasks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    private void loadCompletedTasks() {
        executor.execute(() -> {
            List<Task> tasks = AppDatabase.getInstance(requireContext())
                    .taskDao().getCompletedTasks();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    taskList.clear();
                    taskList.addAll(tasks);
                    historyAdapter.updateTasks(taskList);
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            textEmptyHistory.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
        } else {
            textEmptyHistory.setVisibility(View.GONE);
            recyclerHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRestore(Task task, int position) {
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().restoreTask(task.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    historyAdapter.removeTask(position);
                    updateEmptyState();
                });
            }
        });
    }

    @Override
    public void onDelete(Task task, int position) {
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).taskDao().deleteTask(task.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    historyAdapter.removeTask(position);
                    updateEmptyState();
                });
            }
        });
    }
}
