package com.example.app_ans.tasks.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.R;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.notifications.NotificationViewModel;

public class TaskDetailActivity extends AppCompatActivity {
    private Task task;
    private NotificationViewModel notificationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_detail);

        // Initialize AuthViewModel for logout
        com.example.app_ans.auth.session.AuthSessionManager sessionManager = new com.example.app_ans.auth.session.AuthSessionManager(this);
        com.example.app_ans.auth.repository.AuthRepository authRepository = new com.example.app_ans.auth.repository.AuthRepository(
                com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        com.example.app_ans.auth.ui.AuthViewModel authViewModel = new ViewModelProvider(this, new com.example.app_ans.auth.ui.AuthViewModelFactory(authRepository))
                .get(com.example.app_ans.auth.ui.AuthViewModel.class);

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        handleIntent(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent, null);
    }

    private void handleIntent(android.content.Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            TaskDetailFragment fragment = null;
            String title = "Detalle";

            if (intent.hasExtra("TASK_DATA")) {
                task = (Task) intent.getSerializableExtra("TASK_DATA");
                title = task != null ? task.getPublicId() : "Detalle";
                fragment = TaskDetailFragment.newInstance(task);
            } else if (intent.hasExtra("TASK_ID") || intent.hasExtra("task_id")) {
                int taskId = intent.getIntExtra("TASK_ID", intent.getIntExtra("task_id", -1));
                
                // If it was passed as a String
                if (taskId == -1) {
                    String taskIdStr = intent.getStringExtra("TASK_ID");
                    if (taskIdStr == null) taskIdStr = intent.getStringExtra("task_id");
                    
                    if (taskIdStr != null) {
                        try {
                            taskId = Integer.parseInt(taskIdStr);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (taskId != -1) {
                    fragment = TaskDetailFragment.newInstance(taskId);
                }
            }

            if (fragment != null) {
                // Setup Navbar using utility with logout
                com.example.app_ans.core.ui.NavbarUtils.setupNavbar(this, title, this::showNotificationsBottomSheet, null);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            } else if (savedInstanceState == null) {
                Toast.makeText(this, "Error cargando la tarea", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void showNotificationsBottomSheet() {
        com.example.app_ans.notifications.NotificationsBottomSheetFragment bottomSheet =
                new com.example.app_ans.notifications.NotificationsBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "notifications_bottom_sheet");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationViewModel != null) {
            notificationViewModel.updateUnreadCount();
        }
    }
}