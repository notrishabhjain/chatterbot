package com.taskflow.automate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.automate.model.TeamMember;

import java.util.List;

@Dao
public interface TeamMemberDao {

    @Insert
    long insertMember(TeamMember member);

    @Update
    void updateMember(TeamMember member);

    @Delete
    void deleteMember(TeamMember member);

    @Query("SELECT * FROM team_members ORDER BY name ASC")
    List<TeamMember> getAllMembers();

    @Query("SELECT * FROM team_members WHERE name LIKE '%' || :name || '%'")
    List<TeamMember> getMembersByName(String name);

    @Query("SELECT * FROM team_members WHERE id = :id")
    TeamMember getMemberById(long id);
}
