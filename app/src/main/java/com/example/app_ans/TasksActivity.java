package com.example.app_ans;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_ans.databinding.ActivityTasksBinding;
import com.example.app_ans.tasks.ui.TaskAdapter;
import com.example.app_ans.tasks.ui.TaskDetailFragment;
import com.example.app_ans.notifications.NotificationViewModel;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.ui.TaskViewModel;
import android.content.Intent;
import java.util.List;

public class TasksActivity extends AppCompatActivity {
    private ActivityTasksBinding binding;
    private TaskViewModel viewModel;
    private NotificationViewModel notificationViewModel;
    private TaskAdapter adapter;
    private boolean isTwoPane;
    private int currentTaskCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTasksBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // Initialize AuthViewModel for logout in Navbar
        com.example.app_ans.auth.session.AuthSessionManager sessionManager = new com.example.app_ans.auth.session.AuthSessionManager(this);
        com.example.app_ans.auth.repository.AuthRepository authRepository = new com.example.app_ans.auth.repository.AuthRepository(
                com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        com.example.app_ans.auth.ui.AuthViewModel authViewModel = new ViewModelProvider(this, new com.example.app_ans.auth.ui.AuthViewModelFactory(authRepository))
                .get(com.example.app_ans.auth.ui.AuthViewModel.class);

        // Setup Navbar using utility with logout action
        com.example.app_ans.core.ui.NavbarUtils.setupNavbar(this, "Tareas", this::showNotificationsBottomSheet, () -> authViewModel.logout(this));

        // Observe logout result
        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        isTwoPane = binding.detailContainer != null;

        setupRecyclerView();
        setupViewModel();
        setupListeners();

        viewModel.loadTasks();

        // Handle possible task selection from notification
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && (intent.hasExtra("TASK_ID") || intent.hasExtra("task_id"))) {
            int taskId = intent.getIntExtra("TASK_ID", intent.getIntExtra("task_id", -1));
            
            // If it was passed as a String (common in FCM)
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
                // Direct redirection to DetailActivity, let it load the task
                Intent detailIntent = new Intent(this, com.example.app_ans.tasks.ui.TaskDetailActivity.class);
                detailIntent.putExtra("TASK_ID", taskId);
                startActivity(detailIntent);

                // Clear extra to avoid re-processing
                intent.removeExtra("TASK_ID");
                intent.removeExtra("task_id");
            }
        }
    }

    private void openTaskDetail(Task task) {
        if (isTwoPane) {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit();
        } else {
            openTaskDetailFull(task);
        }
    }

    private void openTaskDetailFull(Task task) {
        Intent intent = new Intent(this, com.example.app_ans.tasks.ui.TaskDetailActivity.class);
        intent.putExtra("TASK_DATA", task);
        startActivity(intent);
    }

    private void showNotificationsBottomSheet() {
        com.example.app_ans.notifications.NotificationsBottomSheetFragment bottomSheet =
            new com.example.app_ans.notifications.NotificationsBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "notifications_bottom_sheet");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadTasks();
        if (notificationViewModel != null) notificationViewModel.updateUnreadCount();
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(task -> openTaskDetail(task));
        binding.tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.tasksRecycler.setAdapter(adapter);

        // Hide banner when clicking it and refresh
        binding.newTasksBanner.setOnClickListener(v -> {
            binding.newTasksBanner.setVisibility(View.GONE);
            binding.tasksRecycler.smoothScrollToPosition(0);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        
        viewModel.getTasks().observe(this, tasks -> {
            int newCount = (tasks != null) ? tasks.size() : 0;

            if (tasks == null || tasks.isEmpty()) {
                adapter.setTasks(java.util.Collections.emptyList());
                binding.tasksRecycler.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.newTasksBanner.setVisibility(View.GONE);
            } else {
                // Determine if we should show the "New Tasks" banner
                // If count increased and we aren't at the very top
                if (newCount > currentTaskCount && currentTaskCount > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) binding.tasksRecycler.getLayoutManager();
                    if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() > 0) {
                        binding.newTasksBanner.setVisibility(View.VISIBLE);
                    }
                }
                
                adapter.setTasks(tasks);
                binding.tasksRecycler.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
            }
            currentTaskCount = newCount;
        });

        viewModel.getLoading().observe(this, loading -> {
            binding.loadingTasks.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Observe notification badge
        notificationViewModel.getUnreadCount().observe(this, count -> {
            // Badge is now managed by the widget
        });
        notificationViewModel.loadNotifications();
    }

    private void setupListeners() {
        // Set query hint programmatically
        binding.searchBar.searchView.setQueryHint("Buscar por nombre, ID o descripción...");

        // Search bar listener
        binding.searchBar.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }
}