package com.example.app_ans.tasks.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.app_ans.tasks.model.Task;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    List<TaskEntity> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getTaskById(int taskId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTasks(List<TaskEntity> tasks);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(TaskEntity task);

    @Query("DELETE FROM tasks")
    void deleteAll();
}
