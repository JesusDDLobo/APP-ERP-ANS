package com.example.app_ans.notifications;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<AppNotification> getAllNotifications();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();

    @Insert
    long insertNotification(AppNotification notification);

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);

    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    void deleteNotification(int notificationId);
}
