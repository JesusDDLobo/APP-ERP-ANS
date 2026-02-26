package com.example.app_ans.tasks.persistence;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_location_updates")
public class PendingLocationUpdate {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int taskId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String status; // PENDING, SYNCED, ERROR

    public PendingLocationUpdate(int taskId, double latitude, double longitude, long timestamp) {
        this.taskId = taskId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.status = "PENDING";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

