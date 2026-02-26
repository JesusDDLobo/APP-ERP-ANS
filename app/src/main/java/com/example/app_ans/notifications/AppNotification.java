package com.example.app_ans.notifications;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "notifications")
public class AppNotification {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String message;
    public String type; // "task_assigned", "task_completed", etc.
    public int relatedTaskId;
    public long timestamp;
    public boolean isRead;

    public AppNotification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    @Ignore
    public AppNotification(String title, String message, String type, int relatedTaskId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedTaskId = relatedTaskId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }
}
