package com.example.app_ans.tasks.persistence;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey
    public int id;
    public String publicId;
    public String createdAt;
    
    // Complex objects stored as JSON strings
    public String stateJson;
    public String contentJson;
    public String assignedUsersJson;
    public String advancesJson;
    public String previousTicketJson;
    public String questionsJson;
    public String timeLogsJson;
    public String vehicleJson;
    public String vehiclePlate;
    public boolean isRunning;
    public String totalTimeSpent;

    public TaskEntity() {}
}
