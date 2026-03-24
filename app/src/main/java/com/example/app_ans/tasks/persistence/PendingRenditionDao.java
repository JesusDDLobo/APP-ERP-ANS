package com.example.app_ans.tasks.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingRenditionDao {

    @Insert
    void insert(PendingRendition rendition);

    @Query("SELECT * FROM pending_renditions WHERE taskId = :taskId ORDER BY createdAt DESC")
    List<PendingRendition> getByTaskId(int taskId);

    @Query("SELECT * FROM pending_renditions ORDER BY createdAt DESC")
    List<PendingRendition> getAll();

    @Query("SELECT COUNT(*) FROM pending_renditions WHERE taskId = :taskId AND createdAt BETWEEN :startOfDay AND :endOfDay")
    int countByTaskIdAndDay(int taskId, long startOfDay, long endOfDay);

    @Query("DELETE FROM pending_renditions WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM pending_renditions WHERE taskId = :taskId")
    void deleteByTaskId(int taskId);
}