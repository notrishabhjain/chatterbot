package com.taskflow.automate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.taskflow.automate.model.Tag;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.model.TaskTagCrossRef;

import java.util.List;

@Dao
public interface TagDao {

    @Insert
    long insertTag(Tag tag);

    @Delete
    void deleteTag(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<Tag> getAllTags();

    @Query("SELECT * FROM tags WHERE id = :id")
    Tag getTagById(long id);

    @Insert
    void insertTaskTagCrossRef(TaskTagCrossRef crossRef);

    @Delete
    void deleteTaskTagCrossRef(TaskTagCrossRef crossRef);

    @Query("SELECT tags.* FROM tags INNER JOIN task_tag_cross_ref ON tags.id = task_tag_cross_ref.tagId WHERE task_tag_cross_ref.taskId = :taskId")
    List<Tag> getTagsForTask(long taskId);

    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tag_cross_ref ON tasks.id = task_tag_cross_ref.taskId WHERE task_tag_cross_ref.tagId = :tagId")
    List<Task> getTasksForTag(long tagId);
}
