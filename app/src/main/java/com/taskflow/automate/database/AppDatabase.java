package com.taskflow.automate.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.taskflow.automate.model.Tag;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.model.TaskTagCrossRef;
import com.taskflow.automate.model.TeamMember;

@Database(entities = {Task.class, TeamMember.class, Tag.class, TaskTagCrossRef.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TaskDao taskDao();

    public abstract TeamMemberDao teamMemberDao();

    public abstract TagDao tagDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "taskflow_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
