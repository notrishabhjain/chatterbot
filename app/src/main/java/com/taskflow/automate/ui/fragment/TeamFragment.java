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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.TeamMember;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeamFragment extends Fragment {

    private RecyclerView recyclerTeam;
    private TextView textEmptyTeam;
    private FloatingActionButton fabAddMember;
    private List<TeamMember> memberList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TeamFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_team, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerTeam = view.findViewById(R.id.recycler_team);
        textEmptyTeam = view.findViewById(R.id.text_empty_team);
        fabAddMember = view.findViewById(R.id.fab_add_member);

        memberList = new ArrayList<>();
        recyclerTeam.setLayoutManager(new LinearLayoutManager(requireContext()));

        fabAddMember.setOnClickListener(v -> {
            // Add member dialog will be implemented in FEAT-004
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMembers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadMembers() {
        executor.execute(() -> {
            List<TeamMember> members = AppDatabase.getInstance(requireContext())
                    .teamMemberDao().getAllMembers();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    memberList.clear();
                    memberList.addAll(members);
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (memberList.isEmpty()) {
            textEmptyTeam.setVisibility(View.VISIBLE);
            recyclerTeam.setVisibility(View.GONE);
        } else {
            textEmptyTeam.setVisibility(View.GONE);
            recyclerTeam.setVisibility(View.VISIBLE);
        }
    }
}
