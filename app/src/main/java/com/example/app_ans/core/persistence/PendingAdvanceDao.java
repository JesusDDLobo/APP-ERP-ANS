package com.example.app_ans.core.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PendingAdvanceDao {
    @Insert
    long insert(PendingAdvance advance);

    @Update
    void update(PendingAdvance advance);

    @Delete
    void delete(PendingAdvance advance);

    @Query("SELECT * FROM pending_advances WHERE status = 'PENDING' OR status = 'ERROR' ORDER BY createdAt ASC")
    List<PendingAdvance> getAllPending();

    @Query("SELECT * FROM pending_advances WHERE taskId = :taskId ORDER BY createdAt DESC")
    List<PendingAdvance> getByTaskId(int taskId);
    
    @Query("SELECT * FROM pending_advances WHERE id = :id")
    PendingAdvance getById(int id);
}
