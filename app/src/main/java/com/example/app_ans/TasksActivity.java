package com.example.app_ans;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.databinding.ActivityTasksBinding;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.ui.TaskAdapter;
import com.example.app_ans.tasks.ui.TaskDetailActivity;
import com.example.app_ans.tasks.ui.TaskDetailFragment;
import com.example.app_ans.tasks.ui.TaskViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TasksActivity extends AppCompatActivity {
    private ActivityTasksBinding binding;
    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private boolean isTwoPane;
    private int currentTaskCount = 0;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTasksBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        com.example.app_ans.auth.session.AuthSessionManager sessionManager =
                new com.example.app_ans.auth.session.AuthSessionManager(this);

        UserSession session = sessionManager.restoreSession();
        String role = session != null ? session.getRole() : null;
        isAdmin = isAdminRole(role);

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
                "Tareas",
                () -> authViewModel.logout(this)
        );

        binding.navbar.navbarBackButton.setVisibility(View.GONE);
        binding.navbar.navbarTitle.setVisibility(View.GONE);

        if (binding.backToHome != null) {
            binding.backToHome.setOnClickListener(v -> finish());
        }

        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        isTwoPane = binding.detailContainer != null;

        if (!isAdmin) {
            showNavbarOnly();
            return;
        }

        setupRecyclerView();
        setupViewModel();
        setupListeners();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (!isAdmin) {
            return;
        }

        handleIntent(intent);
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("administrador") || normalized.equals("admin");
    }

    private void showNavbarOnly() {
        if (binding.backToHome != null) {
            binding.backToHome.setVisibility(View.VISIBLE);
            binding.backToHome.setOnClickListener(v -> finish());
        }

        if (binding.tasksSummaryCard != null) {
            binding.tasksSummaryCard.setVisibility(View.GONE);
        }

        if (binding.searchBar != null) {
            binding.searchBar.getRoot().setVisibility(View.GONE);
        }

        if (binding.tasksRecycler != null) {
            binding.tasksRecycler.setVisibility(View.GONE);
        }

        if (binding.newTasksBanner != null) {
            binding.newTasksBanner.setVisibility(View.GONE);
        }

        if (binding.completedGroupsContainer != null) {
            binding.completedGroupsContainer.setVisibility(View.GONE);
        }

        if (binding.emptyView != null) {
            binding.emptyView.setVisibility(View.GONE);
        }

        if (binding.loadingTasks != null) {
            binding.loadingTasks.setVisibility(View.GONE);
        }

        if (binding.detailContainer != null) {
            binding.detailContainer.setVisibility(View.GONE);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && (intent.hasExtra("TASK_ID") || intent.hasExtra("task_id"))) {
            int taskId = intent.getIntExtra("TASK_ID", intent.getIntExtra("task_id", -1));

            if (taskId == -1) {
                String taskIdStr = intent.getStringExtra("TASK_ID");
                if (taskIdStr == null) taskIdStr = intent.getStringExtra("task_id");

                if (taskIdStr != null) {
                    try {
                        taskId = Integer.parseInt(taskIdStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (taskId != -1) {
                Intent detailIntent = new Intent(this, TaskDetailActivity.class);
                detailIntent.putExtra("TASK_ID", taskId);
                startActivity(detailIntent);

                intent.removeExtra("TASK_ID");
                intent.removeExtra("task_id");
            }
        }
    }

    private void openTaskDetail(Task task) {
        if (!isAdmin) {
            return;
        }

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
        if (!isAdmin) {
            return;
        }

        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("TASK_DATA", task);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAdmin && viewModel != null) {
            viewModel.loadTasks();
        }
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this::openTaskDetail);
        binding.tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.tasksRecycler.setAdapter(adapter);

        binding.newTasksBanner.setOnClickListener(v -> {
            binding.newTasksBanner.setVisibility(View.GONE);
            binding.tasksRecycler.smoothScrollToPosition(0);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        viewModel.getTasks().observe(this, tasks -> {
            int newCount = (tasks != null) ? tasks.size() : 0;

            if (tasks == null || tasks.isEmpty()) {
                adapter.setTasks(Collections.emptyList());
                binding.tasksRecycler.setVisibility(View.GONE);
                binding.completedGroupsContainer.removeAllViews();
                binding.completedGroupsContainer.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.newTasksBanner.setVisibility(View.GONE);
            } else {
                if (newCount > currentTaskCount && currentTaskCount > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) binding.tasksRecycler.getLayoutManager();
                    if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() > 0) {
                        binding.newTasksBanner.setVisibility(View.VISIBLE);
                    }
                }

                List<Task> inProcessTasks = new ArrayList<>();
                List<Task> completedTasks = new ArrayList<>();

                for (Task task : tasks) {
                    if (isCompleted(task)) {
                        completedTasks.add(task);
                    } else {
                        inProcessTasks.add(task);
                    }
                }

                adapter.setTasks(inProcessTasks);
                binding.tasksRecycler.setVisibility(inProcessTasks.isEmpty() ? View.GONE : View.VISIBLE);

                renderCompletedGroups(completedTasks);

                boolean allEmpty = inProcessTasks.isEmpty() && completedTasks.isEmpty();
                binding.emptyView.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
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
    }

    private void setupListeners() {
        binding.searchBar.searchView.setQueryHint("Buscar por nombre, ID o descripción...");

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

    private boolean isCompleted(Task task) {
        return task.getStatus() != null
                && task.getStatus().equalsIgnoreCase("Completado");
    }

    private String buildGroupKey(Task task) {
        if (task.getContent() != null && task.getContent().getSubWorkOrderId() != null) {
            return "SOT-" + task.getContent().getSubWorkOrderId();
        }
        return "Tareas completadas";
    }

    private Map<String, List<Task>> groupCompletedTasks(List<Task> completedTasks) {
        Map<String, List<Task>> grouped = new LinkedHashMap<>();

        for (Task task : completedTasks) {
            String key = buildGroupKey(task);

            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<>());
            }

            grouped.get(key).add(task);
        }

        return grouped;
    }

    private void renderCompletedGroups(List<Task> completedTasks) {
        binding.completedGroupsContainer.removeAllViews();

        if (completedTasks == null || completedTasks.isEmpty()) {
            binding.completedGroupsContainer.setVisibility(View.GONE);
            return;
        }

        binding.completedGroupsContainer.setVisibility(View.VISIBLE);

        Map<String, List<Task>> grouped = groupCompletedTasks(completedTasks);

        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            View groupView = getLayoutInflater().inflate(
                    R.layout.item_completed_group,
                    binding.completedGroupsContainer,
                    false
            );

            TextView countView = groupView.findViewById(R.id.completed_count);
            TextView titleView = groupView.findViewById(R.id.completed_title);
            TextView keyView = groupView.findViewById(R.id.completed_group_key);
            ImageView arrowView = groupView.findViewById(R.id.completed_arrow);
            LinearLayout itemsContainer = groupView.findViewById(R.id.completed_items_container);
            View header = groupView.findViewById(R.id.completed_header);

            List<Task> groupTasks = entry.getValue();

            countView.setText(String.valueOf(groupTasks.size()));
            titleView.setText("Tareas Completadas");
            keyView.setText(entry.getKey());

            itemsContainer.setVisibility(View.GONE);

            for (Task task : groupTasks) {
                View taskItem = getLayoutInflater().inflate(
                        R.layout.item_task,
                        itemsContainer,
                        false
                );

                bindCompletedTaskItem(taskItem, task);
                itemsContainer.addView(taskItem);
            }

            header.setOnClickListener(v -> {
                if (itemsContainer.getVisibility() == View.VISIBLE) {
                    itemsContainer.setVisibility(View.GONE);
                    arrowView.animate().rotation(0f).setDuration(180).start();
                } else {
                    itemsContainer.setVisibility(View.VISIBLE);
                    arrowView.animate().rotation(180f).setDuration(180).start();
                }
            });

            binding.completedGroupsContainer.addView(groupView);
        }
    }

    private void bindCompletedTaskItem(View itemView, Task task) {
        TextView taskId = itemView.findViewById(R.id.task_id);
        TextView taskName = itemView.findViewById(R.id.task_name);
        TextView taskDate = itemView.findViewById(R.id.task_date);
        TextView taskDescription = itemView.findViewById(R.id.task_description);
        TextView vehiclePlate = itemView.findViewById(R.id.vehicle_plate);
        TextView advancesCount = itemView.findViewById(R.id.advances_count);
        TextView statusChip = itemView.findViewById(R.id.status_chip);

        taskId.setText(task.getPublicId() != null ? task.getPublicId() : "Sin ID");

        String name = task.getContent() != null && task.getContent().getName() != null
                ? task.getContent().getName()
                : "Sin nombre";
        taskName.setText(name);

        String start = task.getContent() != null && task.getContent().getStartTime() != null
                ? task.getContent().getStartTime()
                : "-";
        String end = task.getContent() != null && task.getContent().getEndTime() != null
                ? task.getContent().getEndTime()
                : "-";
        taskDate.setText("Inicio: " + start + "\nFin: " + end);

        if (task.getContent() != null && task.getContent().getDescription() != null
                && !task.getContent().getDescription().trim().isEmpty()) {
            taskDescription.setVisibility(View.VISIBLE);
            taskDescription.setText(task.getContent().getDescription());
        } else {
            taskDescription.setVisibility(View.GONE);
        }

        if (task.getVehiclePlate() != null && !task.getVehiclePlate().trim().isEmpty()) {
            vehiclePlate.setVisibility(View.VISIBLE);
            vehiclePlate.setText("Vehículo asignado: " + task.getVehiclePlate());
        } else {
            vehiclePlate.setVisibility(View.GONE);
        }

        advancesCount.setVisibility(View.GONE);

        statusChip.setText("Completado");
        statusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        statusChip.setTextColor(getResources().getColor(android.R.color.white));

        itemView.setOnClickListener(v -> openTaskDetail(task));
    }
}