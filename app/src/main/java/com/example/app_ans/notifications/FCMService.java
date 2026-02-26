package com.example.app_ans.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.app_ans.R;
import com.example.app_ans.TasksActivity;
import com.example.app_ans.core.persistence.AppDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "ans_tasks_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "New FCM Token generated");

        getSharedPreferences("FCM", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String type = remoteMessage.getData().get("type");


            String ticketIdStr = remoteMessage.getData().get("ticket_id");
            if (ticketIdStr == null) ticketIdStr = remoteMessage.getData().get("task_id");
            if (ticketIdStr == null) ticketIdStr = remoteMessage.getData().get("id");

            int taskId = 0;
            try {
                if (ticketIdStr != null) {
                    taskId = Integer.parseInt(ticketIdStr);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid ticket_id: " + ticketIdStr);
            }

            saveNotificationToDatabase(title, message, type, taskId);
            showNotification(title, message, taskId);
        }

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, 0);
        }
    }

    private void saveNotificationToDatabase(String title, String message, String type, int taskId) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                AppNotification notification = new AppNotification(title, message, type, taskId);
                db.notificationDao().insertNotification(notification);
                Log.d(TAG, "Notification saved to DB");
            } catch (Exception e) {
                Log.e(TAG, "Error saving notification", e);
            }
        }).start();
    }

    private void showNotification(String title, String message, int taskId) {
        try {
            createNotificationChannel();

            // Always go to TasksActivity first to ensure proper backstack
            Intent intent = new Intent(this, TasksActivity.class);
            if (taskId > 0) {
                intent.putExtra("TASK_ID", taskId);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                taskId, // Use taskId or a constant
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, 
                        android.Manifest.permission.POST_NOTIFICATIONS) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(taskId, builder.build());
                    }
                } else {
                    notificationManager.notify(taskId, builder.build());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Tareas Asignadas",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de nuevas tareas asignadas");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
