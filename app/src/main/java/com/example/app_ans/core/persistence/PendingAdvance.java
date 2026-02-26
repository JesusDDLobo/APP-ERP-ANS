package com.example.app_ans.core.persistence;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

@Entity(tableName = "pending_advances")
public class PendingAdvance {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int taskId;
    private String content;
    
    @TypeConverters(StringListConverter.class)
    private List<String> filePaths;
    
    private long createdAt;
    private String status; // PENDING, UPLOADING, ERROR

    public PendingAdvance(int taskId, String content, List<String> filePaths) {
        this.taskId = taskId;
        this.content = content;
        this.filePaths = filePaths;
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getFilePaths() { return filePaths; }
    public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
