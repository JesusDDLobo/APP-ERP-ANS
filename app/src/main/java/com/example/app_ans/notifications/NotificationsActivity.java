package com.example.app_ans.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_ans.R;
import com.example.app_ans.TasksActivity;
import com.example.app_ans.tasks.ui.TaskDetailActivity;

public class NotificationsActivity extends AppCompatActivity {
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Initialize AuthViewModel for logout
        com.example.app_ans.auth.session.AuthSessionManager sessionManager = new com.example.app_ans.auth.session.AuthSessionManager(this);
        com.example.app_ans.auth.repository.AuthRepository repository = new com.example.app_ans.auth.repository.AuthRepository(
                com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        com.example.app_ans.auth.ui.AuthViewModel authViewModel = new ViewModelProvider(this, new com.example.app_ans.auth.ui.AuthViewModelFactory(repository))
                .get(com.example.app_ans.auth.ui.AuthViewModel.class);

        // Setup Navbar using utility with logout
        com.example.app_ans.core.ui.NavbarUtils.setupNavbar(this, "Notificaciones", this::showNotificationsBottomSheet, () -> authViewModel.logout(this));

        // Observe logout result
        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        recyclerView = findViewById(R.id.notifications_recycler);
        emptyView = findViewById(R.id.empty_view);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Setup RecyclerView
        adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(AppNotification notification) {
                viewModel.markAsRead(notification.id);

                // Navigate to task if it's a task assignment
                if (notification.relatedTaskId > 0) {
                    Intent intent = new Intent(NotificationsActivity.this, TaskDetailActivity.class);
                    intent.putExtra("TASK_ID", notification.relatedTaskId);
                    startActivity(intent);
                }
            }

            @Override
            public void onDeleteClick(AppNotification notification) {
                viewModel.deleteNotification(notification.id);
                Toast.makeText(NotificationsActivity.this, "Notificación eliminada", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Observe notifications
        viewModel.getNotifications().observe(this, notifications -> {
            if (notifications == null || notifications.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.setNotifications(notifications);
            }
        });

        // Load notifications
        viewModel.loadNotifications();

        // Hide the notification button on the notifications screen itself
        View notificationButton = findViewById(R.id.navbar_notification_button);
        if (notificationButton != null) {
            notificationButton.setVisibility(View.GONE);
        }
    }

    private void showNotificationsBottomSheet() {
        NotificationsBottomSheetFragment bottomSheet = new NotificationsBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "notifications_bottom_sheet");
    }
}
