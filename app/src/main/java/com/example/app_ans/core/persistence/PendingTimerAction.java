package com.example.app_ans.core.persistence;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_timer_actions")
public class PendingTimerAction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int taskId;
    public String action; // "TOGGLE"
    public long timestamp;
    public String totalTimeAtAction; // Time shown in UI when action was recorded

    public PendingTimerAction(int taskId, String action, long timestamp) {
        this.taskId = taskId;
        this.action = action;
        this.timestamp = timestamp;
    }
}
