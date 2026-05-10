package com.taskflow.automate.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.automate.R;
import com.taskflow.automate.model.TeamMember;

import java.util.List;

public class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.TeamMemberViewHolder> {

    public interface OnTeamMemberActionListener {
        void onEdit(TeamMember member, int position);
        void onDelete(TeamMember member, int position);
    }

    private List<TeamMember> members;
    private final OnTeamMemberActionListener listener;

    public TeamMemberAdapter(List<TeamMember> members, OnTeamMemberActionListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeamMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new TeamMemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamMemberViewHolder holder, int position) {
        TeamMember member = members.get(position);
        holder.bind(member, position);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void updateMembers(List<TeamMember> newMembers) {
        this.members = newMembers;
        notifyDataSetChanged();
    }

    class TeamMemberViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textEmail;
        private final TextView textPhone;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        TeamMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_member_name);
            textEmail = itemView.findViewById(R.id.text_member_email);
            textPhone = itemView.findViewById(R.id.text_member_phone);
            btnEdit = itemView.findViewById(R.id.btn_edit_member);
            btnDelete = itemView.findViewById(R.id.btn_delete_member);
        }

        void bind(TeamMember member, int position) {
            textName.setText(member.getName());

            if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                textEmail.setText(member.getEmail());
                textEmail.setVisibility(View.VISIBLE);
            } else {
                textEmail.setVisibility(View.GONE);
            }

            if (member.getPhone() != null && !member.getPhone().isEmpty()) {
                textPhone.setText(member.getPhone());
                textPhone.setVisibility(View.VISIBLE);
            } else {
                textPhone.setVisibility(View.GONE);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(member, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(member, position);
                }
            });
        }
    }
}
