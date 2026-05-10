package com.taskflow.automate.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.model.TeamMember;
import com.taskflow.automate.ui.adapter.ActionItemAdapter;
import com.taskflow.automate.util.MeetingTaskExtractor;
import com.taskflow.automate.util.PriorityAssigner;
import com.taskflow.automate.util.TranscriptParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportFragment extends Fragment {

    private MaterialButton btnPasteClipboard;
    private MaterialButton btnPickFile;
    private TextInputEditText editTranscript;
    private MaterialButton btnParse;
    private RecyclerView recyclerParsedItems;
    private MaterialButton btnCreateTasks;
    private ActionItemAdapter actionItemAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PriorityAssigner priorityAssigner = new PriorityAssigner();

    private ActivityResultLauncher<Intent> filePickerLauncher;

    public ImportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            readFileContent(uri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnPasteClipboard = view.findViewById(R.id.btn_paste_clipboard);
        btnPickFile = view.findViewById(R.id.btn_pick_file);
        editTranscript = view.findViewById(R.id.edit_transcript);
        btnParse = view.findViewById(R.id.btn_parse);
        recyclerParsedItems = view.findViewById(R.id.recycler_parsed_items);
        btnCreateTasks = view.findViewById(R.id.btn_create_tasks);

        recyclerParsedItems.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnPasteClipboard.setOnClickListener(v -> pasteFromClipboard());
        btnPickFile.setOnClickListener(v -> pickFile());
        btnParse.setOnClickListener(v -> parseTranscript());
        btnCreateTasks.setOnClickListener(v -> createTasks());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                if (text != null) {
                    editTranscript.setText(text);
                }
            }
        }
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        filePickerLauncher.launch(intent);
    }

    private void readFileContent(Uri uri) {
        executor.execute(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();
                    inputStream.close();
                    String content = sb.toString();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> editTranscript.setText(content));
                    }
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error reading file", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseTranscript() {
        String text = editTranscript.getText() != null ? editTranscript.getText().toString() : "";
        if (text.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter or paste a transcript first", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            // Load team members for assignee detection
            List<TeamMember> members = AppDatabase.getInstance(requireContext())
                    .teamMemberDao().getAllMembers();
            List<String> memberNames = new ArrayList<>();
            for (TeamMember member : members) {
                memberNames.add(member.getName());
            }

            // Parse transcript
            TranscriptParser parser = new TranscriptParser();
            parser.setTeamMembers(memberNames);
            List<MeetingTaskExtractor.ExtractedActionItem> items = parser.parseTranscript(text);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (items.isEmpty()) {
                        Toast.makeText(requireContext(), "No action items found", Toast.LENGTH_SHORT).show();
                        recyclerParsedItems.setVisibility(View.GONE);
                        btnCreateTasks.setVisibility(View.GONE);
                    } else {
                        actionItemAdapter = new ActionItemAdapter(items);
                        recyclerParsedItems.setAdapter(actionItemAdapter);
                        recyclerParsedItems.setVisibility(View.VISIBLE);
                        btnCreateTasks.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void createTasks() {
        if (actionItemAdapter == null) return;

        List<MeetingTaskExtractor.ExtractedActionItem> selectedItems = actionItemAdapter.getSelectedItems();
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            int createdCount = 0;
            for (MeetingTaskExtractor.ExtractedActionItem item : selectedItems) {
                Task task = new Task();
                task.setTitle(item.title);
                task.setSourceApp("Transcript Import");
                task.setStatus("pending");
                task.setCreatedAt(System.currentTimeMillis());
                task.setDueDate(item.dueDate);
                task.setAssignee(item.assigneeName);

                // Use PriorityAssigner for priority
                int priority = priorityAssigner.assignPriority(
                        null, item.title, item.rawText, item.dueDate);
                task.setPriority(priority);

                long taskId = AppDatabase.getInstance(requireContext())
                        .taskDao().insertTask(task);
                createdCount++;

                // If assignee is detected, create a follow-up task
                if (item.assigneeName != null && !item.assigneeName.isEmpty()) {
                    Task followUp = new Task();
                    followUp.setTitle("Follow up: " + item.title + " with " + item.assigneeName);
                    followUp.setSourceApp("Transcript Import");
                    followUp.setStatus("pending");
                    followUp.setCreatedAt(System.currentTimeMillis());
                    followUp.setPriority(priority);
                    followUp.setFollowUp(true);
                    followUp.setLinkedTaskId(taskId);

                    // Due date = main task due date + 1 day, or null if no due date
                    if (item.dueDate != null) {
                        followUp.setDueDate(item.dueDate + 24 * 60 * 60 * 1000L);
                    }

                    AppDatabase.getInstance(requireContext())
                            .taskDao().insertTask(followUp);
                    createdCount++;
                }
            }

            int finalCount = createdCount;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            finalCount + " task(s) created", Toast.LENGTH_SHORT).show();
                    // Clear the form
                    editTranscript.setText("");
                    recyclerParsedItems.setVisibility(View.GONE);
                    btnCreateTasks.setVisibility(View.GONE);
                    actionItemAdapter = null;
                });
            }
        });
    }
}
