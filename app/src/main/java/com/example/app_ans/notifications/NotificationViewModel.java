package com.example.app_ans.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_ans.core.persistence.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        this.db = AppDatabase.getInstance(application);
    }

    public LiveData<List<AppNotification>> getNotifications() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCountLiveData;
    }

    public void loadNotifications() {
        executor.execute(() -> {
            List<AppNotification> notifications = db.notificationDao().getAllNotifications();
            notificationsLiveData.postValue(notifications);
            updateUnreadCount();
        });
    }

    public void updateUnreadCount() {
        executor.execute(() -> {
            int count = db.notificationDao().getUnreadCount();
            unreadCountLiveData.postValue(count);
        });
    }

    public void markAsRead(int notificationId) {
        executor.execute(() -> {
            db.notificationDao().markAsRead(notificationId);
            updateUnreadCount();
            loadNotifications();
        });
    }

    public void markAllAsRead() {
        executor.execute(() -> {
            db.notificationDao().markAllAsRead();
            updateUnreadCount();
            loadNotifications();
        });
    }

    public void deleteNotification(int notificationId) {
        executor.execute(() -> {
            db.notificationDao().deleteNotification(notificationId);
            loadNotifications();
        });
    }
}

