package com.example.app_ans.tasks.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.R;
import com.example.app_ans.tasks.model.Task;

public class TaskDetailActivity extends AppCompatActivity {
    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_detail);

        handleIntent(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent, null);
    }

    private void handleIntent(Intent intent, Bundle savedInstanceState) {
        if (intent == null) return;

        TaskDetailFragment fragment = null;

        if (intent.hasExtra("TASK_DATA")) {
            task = (Task) intent.getSerializableExtra("TASK_DATA");
            fragment = TaskDetailFragment.newInstance(task);
        } else if (intent.hasExtra("TASK_ID") || intent.hasExtra("task_id")) {
            int taskId = intent.getIntExtra("TASK_ID", intent.getIntExtra("task_id", -1));

            if (taskId == -1) {
                String taskIdStr = intent.getStringExtra("TASK_ID");
                if (taskIdStr == null) {
                    taskIdStr = intent.getStringExtra("task_id");
                }

                if (taskIdStr != null) {
                    try {
                        taskId = Integer.parseInt(taskIdStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (taskId != -1) {
                fragment = TaskDetailFragment.newInstance(taskId);
            }
        }

        if (fragment == null) {
            if (savedInstanceState == null) {
                Toast.makeText(this, "Error cargando la tarea", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        setupNavbar();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setupNavbar() {
        com.example.app_ans.auth.session.AuthSessionManager sessionManager =
                new com.example.app_ans.auth.session.AuthSessionManager(this);

        com.example.app_ans.auth.repository.AuthRepository authRepository =
                new com.example.app_ans.auth.repository.AuthRepository(
                        com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                        sessionManager
                );

        com.example.app_ans.auth.ui.AuthViewModel authViewModel =
                new ViewModelProvider(
                        this,
                        new com.example.app_ans.auth.ui.AuthViewModelFactory(authRepository)
                ).get(com.example.app_ans.auth.ui.AuthViewModel.class);

        com.example.app_ans.core.ui.NavbarUtils.setupNavbar(
                this,
                null,
                () -> authViewModel.logout(this)
        );

        View navbarBackButton = findViewById(R.id.navbar_back_button);
        if (navbarBackButton != null) {
            navbarBackButton.setVisibility(View.GONE);
        }

        View navbarTitle = findViewById(R.id.navbar_title);
        if (navbarTitle != null) {
            navbarTitle.setVisibility(View.GONE);
        }

        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent logoutIntent = new Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                logoutIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );
                startActivity(logoutIntent);
                finish();
            }
        });
    }
}