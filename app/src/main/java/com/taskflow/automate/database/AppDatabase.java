package com.taskflow.automate.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.taskflow.automate.model.Task;

@Database(entities = {Task.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN assigner TEXT");
            database.execSQL("ALTER TABLE tasks ADD COLUMN task_type TEXT");
            database.execSQL("ALTER TABLE tasks ADD COLUMN is_follow_up INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN source_notification_text TEXT");
        }
    };

    public abstract TaskDao taskDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "taskflow_database"
                    )
                    .addMigrations(MIGRATION_2_3)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
