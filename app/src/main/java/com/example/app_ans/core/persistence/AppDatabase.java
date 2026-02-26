package com.example.app_ans.core.persistence;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.app_ans.tasks.persistence.TaskDao;
import com.example.app_ans.tasks.persistence.TaskEntity;
import com.example.app_ans.tasks.persistence.TaskDraft;
import com.example.app_ans.tasks.persistence.TaskDraftDao;
import com.example.app_ans.tasks.persistence.PendingLocationUpdate;
import com.example.app_ans.tasks.persistence.PendingLocationUpdateDao;
import com.example.app_ans.notifications.AppNotification;
import com.example.app_ans.notifications.NotificationDao;

@Database(entities = {
    PendingAdvance.class,
    TaskEntity.class,
    AppNotification.class,
    TaskDraft.class,
    PendingTimerAction.class,
    PendingLocationUpdate.class
}, version = 10, exportSchema = false)
@TypeConverters({StringListConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract PendingAdvanceDao pendingAdvanceDao();
    public abstract TaskDao taskDao();
    public abstract NotificationDao notificationDao();
    public abstract TaskDraftDao taskDraftDao();
    public abstract PendingTimerActionDao pendingTimerActionDao();
    public abstract PendingLocationUpdateDao pendingLocationUpdateDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ans_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
