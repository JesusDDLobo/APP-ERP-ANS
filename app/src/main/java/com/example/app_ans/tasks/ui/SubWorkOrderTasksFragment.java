package com.example.app_ans.tasks.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.R;
import com.example.app_ans.core.utils.DateUtils;
import com.example.app_ans.tasks.model.Task;

import java.util.List;

public class SubWorkOrderTasksFragment extends Fragment {

    private int subWorkOrderId = -1;
    private String subWorkOrderPublicId;
    private String subWorkOrderName;

    private TaskViewModel viewModel;

    private TextView titleView;
    private TextView subtitleView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private LinearLayout tasksContainer;

    public SubWorkOrderTasksFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_work_order_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            subWorkOrderId = args.getInt("sub_work_order_id", -1);
            subWorkOrderPublicId = args.getString("sub_work_order_public_id");
            subWorkOrderName = args.getString("sub_work_order_name");
        }

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        titleView = view.findViewById(R.id.tv_sub_work_order_title);
        subtitleView = view.findViewById(R.id.tv_sub_work_order_subtitle);
        progressBar = view.findViewById(R.id.progress_sub_work_order_tasks);
        emptyView = view.findViewById(R.id.tv_empty_sub_work_order_tasks);
        tasksContainer = view.findViewById(R.id.layout_sub_work_order_tasks_container);

        setupBackButton();
        bindHeader();
        setupObservers();

        if (subWorkOrderId > 0) {
            viewModel.loadSubWorkOrderTasks(subWorkOrderId);
        } else {
            showEmpty("No se encontró el identificador de la suborden.");
        }
    }

    private void bindHeader() {
        String title = subWorkOrderPublicId != null && !subWorkOrderPublicId.trim().isEmpty()
                ? subWorkOrderPublicId
                : "Suborden";

        String subtitle = "Lista de tareas relacionadas";
        if (subWorkOrderName != null && !subWorkOrderName.trim().isEmpty()) {
            subtitle = subWorkOrderName;
        }

        titleView.setText(title);
        subtitleView.setText(subtitle);
    }

    private void setupObservers() {
        viewModel.getSubWorkOrderTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (!isAdded()) return;
            renderTasks(tasks);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!isAdded()) return;
            progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (!isAdded() || error == null || error.trim().isEmpty()) return;
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });
    }

    private void renderTasks(List<Task> tasks) {
        tasksContainer.removeAllViews();

        if (tasks == null || tasks.isEmpty()) {
            showEmpty("No hay tareas relacionadas a esta suborden.");
            return;
        }

        emptyView.setVisibility(View.GONE);
        tasksContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Task task : tasks) {
            View itemView = inflater.inflate(R.layout.item_task, tasksContainer, false);
            bindTaskItem(itemView, task);
            tasksContainer.addView(itemView);
        }
    }

    private void bindTaskItem(View itemView, Task task) {
        TextView taskId = itemView.findViewById(R.id.task_id);
        TextView taskName = itemView.findViewById(R.id.task_name);
        TextView taskDate = itemView.findViewById(R.id.task_date);
        TextView taskDescription = itemView.findViewById(R.id.task_description);
        TextView vehiclePlate = itemView.findViewById(R.id.vehicle_plate);
        TextView advancesCount = itemView.findViewById(R.id.advances_count);
        TextView statusChip = itemView.findViewById(R.id.status_chip);

        taskId.setText(task.getPublicId() != null ? task.getPublicId() : "Sin ID");

        String name = task.getName() != null && !task.getName().trim().isEmpty()
                ? task.getName()
                : "Sin nombre";
        taskName.setText(name);

        String start = task.getStartTime() != null
                ? DateUtils.formatDateTime(task.getStartTime())
                : "-";
        String end = task.getEndTime() != null
                ? DateUtils.formatDateTime(task.getEndTime())
                : "-";
        taskDate.setText("Inicio: " + start + "\nFin: " + end);

        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            taskDescription.setVisibility(View.VISIBLE);
            taskDescription.setText(task.getDescription());
        } else {
            taskDescription.setVisibility(View.GONE);
        }

        if (task.getVehiclePlate() != null && !task.getVehiclePlate().trim().isEmpty()) {
            vehiclePlate.setVisibility(View.VISIBLE);
            vehiclePlate.setText("Vehículo asignado: " + task.getVehiclePlate());
        } else {
            vehiclePlate.setVisibility(View.GONE);
        }

        int advances = task.getAdvances() != null ? task.getAdvances().size() : 0;
        if (advances > 0) {
            advancesCount.setVisibility(View.VISIBLE);
            advancesCount.setText("Avances: " + advances);
        } else {
            advancesCount.setVisibility(View.GONE);
        }

        String status = task.getStatus() != null ? task.getStatus() : "Sin estado";
        statusChip.setText(status);

        applyStatusStyle(statusChip, status);

        itemView.setClickable(true);
        itemView.setFocusable(true);
        itemView.setOnClickListener(v -> openTaskDetail(task));
    }

    private void openTaskDetail(Task task) {
        if (task == null || !isAdded()) return;

        Toast.makeText(requireContext(), "Abriendo " + task.getPublicId(), Toast.LENGTH_SHORT).show();

        View root = requireView();
        View parent = (View) root.getParent();
        int containerId = parent != null ? parent.getId() : View.NO_ID;

        if (containerId == View.NO_ID) {
            Toast.makeText(requireContext(), "Contenedor inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, TaskDetailFragment.newInstance(task, true))
                .addToBackStack("task_detail")
                .commitAllowingStateLoss();
    }

    private void applyStatusStyle(TextView statusChip, String status) {
        String normalized = status != null ? status.trim().toLowerCase() : "";

        switch (normalized) {
            case "completado":
                statusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                statusChip.setTextColor(requireContext().getResources().getColor(android.R.color.white));
                break;

            case "en proceso":
                statusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
                statusChip.setTextColor(requireContext().getResources().getColor(android.R.color.white));
                break;

            case "pendiente":
                statusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                statusChip.setTextColor(requireContext().getResources().getColor(android.R.color.white));
                break;

            default:
                com.example.app_ans.core.ui.StatusUtils.applyStatusColor(statusChip, status);
                break;
        }
    }

    private void showEmpty(String message) {
        tasksContainer.removeAllViews();
        tasksContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    private void setupBackButton() {
        View activityView = requireActivity().findViewById(R.id.back_to_home);
        if (!(activityView instanceof TextView)) {
            return;
        }

        TextView backText = (TextView) activityView;
        backText.setText("←  Volver a subórdenes");
        backText.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );
    }

    @Override
    public void onDestroyView() {
        View activityView = requireActivity().findViewById(R.id.back_to_home);
        if (activityView instanceof TextView) {
            TextView backText = (TextView) activityView;
            backText.setText("←  Volver al inicio");
            backText.setOnClickListener(v -> requireActivity().finish());
        }

        super.onDestroyView();
    }
}