package com.example.app_ans.tasks.persistence;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_drafts")
public class TaskDraft {
    @PrimaryKey
    @NonNull
    public String draftId; // Format: TYPE_ID (e.g., "ADVANCE_5", "COMPLETION_5")

    public String content; // The JSON or text content of the draft
    public long lastUpdated;

    public TaskDraft(@NonNull String draftId, String content) {
        this.draftId = draftId;
        this.content = content;
        this.lastUpdated = System.currentTimeMillis();
    }
}
