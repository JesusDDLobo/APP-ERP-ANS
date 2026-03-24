package com.example.app_ans.tasks.persistence;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_renditions")
public class PendingRendition {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int taskId;
    public String renditionJson;
    public String localFilesJson;
    public long createdAt;

    public PendingRendition(int taskId, String renditionJson, String localFilesJson, long createdAt) {
        this.taskId = taskId;
        this.renditionJson = renditionJson;
        this.localFilesJson = localFilesJson;
        this.createdAt = createdAt;
    }
}