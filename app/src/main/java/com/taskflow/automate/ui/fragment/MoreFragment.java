package com.taskflow.automate.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.taskflow.automate.R;
import com.taskflow.automate.ui.SettingsActivity;

public class MoreFragment extends Fragment {

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

        cardHistory.setOnClickListener(v -> {
            // History will be implemented in a later feature
            Toast.makeText(requireContext(), "History coming soon", Toast.LENGTH_SHORT).show();
        });

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        cardExport.setOnClickListener(v -> {
            // Export will be implemented in a later feature
            Toast.makeText(requireContext(), "Export coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}
