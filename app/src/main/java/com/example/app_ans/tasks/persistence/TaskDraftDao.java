package com.example.app_ans.tasks.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface TaskDraftDao {
    @Query("SELECT * FROM task_drafts WHERE draftId = :draftId")
    TaskDraft getDraft(String draftId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveDraft(TaskDraft draft);

    @Query("DELETE FROM task_drafts WHERE draftId = :draftId")
    void deleteDraft(String draftId);
}
