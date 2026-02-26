package com.example.app_ans.tasks.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface PendingLocationUpdateDao {
    @Insert
    void insert(PendingLocationUpdate update);

    @Query("SELECT * FROM pending_location_updates WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<PendingLocationUpdate> getAllPending();

    @Update
    void update(PendingLocationUpdate update);

    @Delete
    void delete(PendingLocationUpdate update);

    @Query("DELETE FROM pending_location_updates WHERE status = 'SYNCED'")
    void clearSynced();
}

