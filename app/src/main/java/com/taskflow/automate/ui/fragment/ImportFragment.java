package com.taskflow.automate.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;

public class ImportFragment extends Fragment {

    private MaterialButton btnPasteClipboard;
    private MaterialButton btnPickFile;
    private TextInputEditText editTranscript;
    private MaterialButton btnParse;
    private RecyclerView recyclerParsedItems;
    private MaterialButton btnCreateTasks;

    public ImportFragment() {
        // Required empty public constructor
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

        btnPasteClipboard.setOnClickListener(v -> pasteFromClipboard());
        btnPickFile.setOnClickListener(v -> {
            // File picker will be implemented in FEAT-004
            Toast.makeText(requireContext(), "File picker coming soon", Toast.LENGTH_SHORT).show();
        });
        btnParse.setOnClickListener(v -> {
            // Parsing logic will be implemented in FEAT-004
            Toast.makeText(requireContext(), "Parsing coming soon", Toast.LENGTH_SHORT).show();
        });
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
}
