package com.taskflow.automate.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.TeamMember;
import com.taskflow.automate.ui.adapter.TeamMemberAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeamFragment extends Fragment implements TeamMemberAdapter.OnTeamMemberActionListener {

    private RecyclerView recyclerTeam;
    private TextView textEmptyTeam;
    private FloatingActionButton fabAddMember;
    private List<TeamMember> memberList;
    private TeamMemberAdapter adapter;
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
        adapter = new TeamMemberAdapter(memberList, this);
        recyclerTeam.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTeam.setAdapter(adapter);

        fabAddMember.setOnClickListener(v -> showAddMemberDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMembers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    @Override
    public void onEdit(TeamMember member, int position) {
        showEditMemberDialog(member, position);
    }

    @Override
    public void onDelete(TeamMember member, int position) {
        showDeleteConfirmationDialog(member, position);
    }

    private void loadMembers() {
        executor.execute(() -> {
            List<TeamMember> members = AppDatabase.getInstance(requireContext())
                    .teamMemberDao().getAllMembers();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    memberList.clear();
                    memberList.addAll(members);
                    adapter.updateMembers(memberList);
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

    private void showAddMemberDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_team_member, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_member_name);
        TextInputEditText editEmail = dialogView.findViewById(R.id.edit_member_email);
        TextInputEditText editPhone = dialogView.findViewById(R.id.edit_member_phone);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Team Member")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                    String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TeamMember member = new TeamMember();
                    member.setName(name);
                    member.setEmail(email);
                    member.setPhone(phone);
                    member.setCreatedAt(System.currentTimeMillis());

                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext())
                                .teamMemberDao().insertMember(member);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> loadMembers());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditMemberDialog(TeamMember member, int position) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_team_member, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_member_name);
        TextInputEditText editEmail = dialogView.findViewById(R.id.edit_member_email);
        TextInputEditText editPhone = dialogView.findViewById(R.id.edit_member_phone);

        // Pre-populate with existing data
        editName.setText(member.getName());
        editEmail.setText(member.getEmail());
        editPhone.setText(member.getPhone());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Team Member")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                    String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    member.setName(name);
                    member.setEmail(email);
                    member.setPhone(phone);

                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext())
                                .teamMemberDao().updateMember(member);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> loadMembers());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog(TeamMember member, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Team Member")
                .setMessage("Are you sure you want to delete " + member.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executor.execute(() -> {
                        AppDatabase.getInstance(requireContext())
                                .teamMemberDao().deleteMember(member);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> loadMembers());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
