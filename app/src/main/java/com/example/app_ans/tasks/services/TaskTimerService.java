package com.example.app_ans.tasks.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TaskTimerService extends Service {
    private static final String TAG = "TaskTimerService";
    private static final String CHANNEL_ID = "task_timer_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String EXTRA_TASK_ID = "TASK_ID";
    public static final String EXTRA_TASK_NAME = "TASK_NAME";
    public static final String EXTRA_START_TIME = "START_TIME";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            Log.d(TAG, "Stopping timer service");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);

        // RECIBIMOS LOS SEGUNDOS TOTALES TRANSCURRIDOS
        long totalElapsedSeconds = intent.getLongExtra(EXTRA_START_TIME, 0);

        Log.d(TAG, "Notification Timer for: " + taskName + " | Elapsed: " + totalElapsedSeconds + "s");

        // Calculamos la base para el cronómetro de la notificación restando esos segundos del tiempo actual.
        // setWhen espera Absolute Millis.
        long notificationBaseMillis = System.currentTimeMillis() - (totalElapsedSeconds * 1000);

        showNotification(taskId, taskName, notificationBaseMillis);

        return START_STICKY;
    }

    private void showNotification(int taskId, String taskName, long baseMillis) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, com.example.app_ans.tasks.ui.TaskDetailActivity.class);
        notificationIntent.putExtra("TASK_ID", taskId);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tarea en ejecución")
                .setContentText(taskName != null ? taskName : "Contando tiempo...")
                .setSmallIcon(com.example.app_ans.R.drawable.ic_notification)
                .setUsesChronometer(true)
                // Usamos la base calculada en absoluto. El sistema Android se encarga de mostrar
                // el contador incremental automáticamente partiendo de este punto.
                .setWhen(baseMillis)
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Aumentado para mejor visibilidad
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Cronómetro de Tareas",
                NotificationManager.IMPORTANCE_DEFAULT // Aumentado de LOW a DEFAULT
        );
        channel.setDescription("Muestra el tiempo transcurrido en la tarea actual");
        channel.setShowBadge(true); // Permitir badge

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
