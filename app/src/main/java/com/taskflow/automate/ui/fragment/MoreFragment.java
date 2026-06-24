package com.taskflow.automate.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.ui.EmailConfigActivity;
import com.taskflow.automate.ui.SettingsActivity;
import com.taskflow.automate.util.ExportManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoreFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MoreFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView cardHistory = view.findViewById(R.id.card_history);
        MaterialCardView cardSettings = view.findViewById(R.id.card_settings);
        MaterialCardView cardExport = view.findViewById(R.id.card_export);
        MaterialCardView cardEmailDigest = view.findViewById(R.id.card_email_digest);

        cardHistory.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        cardExport.setOnClickListener(v -> showExportDialog());

        cardEmailDigest.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EmailConfigActivity.class));
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void showExportDialog() {
        String[] options = {
                getString(R.string.export_as_json),
                getString(R.string.export_as_csv)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportTasks("json");
                    } else {
                        exportTasks("csv");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void exportTasks(String format) {
        executor.execute(() -> {
            try {
                List<Task> pendingTasks = AppDatabase.getInstance(requireContext())
                        .taskDao().getPendingTasks();
                List<Task> completedTasks = AppDatabase.getInstance(requireContext())
                        .taskDao().getCompletedTasks();

                List<Task> allTasks = new ArrayList<>();
                allTasks.addAll(pendingTasks);
                allTasks.addAll(completedTasks);

                ExportManager exportManager = new ExportManager();
                String content;
                String filename;
                String mimeType;

                if ("json".equals(format)) {
                    content = exportManager.exportToJson(allTasks);
                    filename = "taskflow_export_" + System.currentTimeMillis() + ".json";
                    mimeType = "application/json";
                } else {
                    content = exportManager.exportToCsv(allTasks);
                    filename = "taskflow_export_" + System.currentTimeMillis() + ".csv";
                    mimeType = "text/csv";
                }

                File file = exportManager.writeToFile(requireContext(), content, filename);
                Uri fileUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", file);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(mimeType);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_export)));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.export_error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
