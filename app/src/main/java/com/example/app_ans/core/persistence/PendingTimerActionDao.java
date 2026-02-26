package com.example.app_ans.core.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface PendingTimerActionDao {
    @Insert
    void insert(PendingTimerAction action);

    @Query("SELECT * FROM pending_timer_actions ORDER BY timestamp ASC")
    List<PendingTimerAction> getAllPending();

    @Query("SELECT * FROM pending_timer_actions WHERE taskId = :taskId ORDER BY timestamp ASC")
    List<PendingTimerAction> getByTaskId(int taskId);

    @Delete
    void delete(PendingTimerAction action);

    @Query("DELETE FROM pending_timer_actions WHERE taskId = :taskId")
    void deleteByTaskId(int taskId);
}

