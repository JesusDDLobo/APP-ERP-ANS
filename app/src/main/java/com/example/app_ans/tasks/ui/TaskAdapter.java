package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.core.utils.DateUtils;
import com.example.app_ans.tasks.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {
    private final OnTaskClickListener listener;
    private List<Task> fullList = new ArrayList<>();
    private String lastQuery = "";

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            boolean sameContent = false;
            if (oldItem.getContent() != null && newItem.getContent() != null) {
                sameContent = java.util.Objects.equals(oldItem.getContent().getName(), newItem.getContent().getName());
            } else if (oldItem.getContent() == null && newItem.getContent() == null) {
                sameContent = true;
            }

            return java.util.Objects.equals(oldItem.getPublicId(), newItem.getPublicId()) &&
                   java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                   oldItem.isRunning() == newItem.isRunning() &&
                   java.util.Objects.equals(oldItem.getTotalTimeSpent(), newItem.getTotalTimeSpent()) &&
                   java.util.Objects.equals(oldItem.getVehiclePlate(), newItem.getVehiclePlate()) &&
                   java.util.Objects.equals(oldItem.getVehicle(), newItem.getVehicle()) &&
                   sameContent;
        }
    };

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.fullList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        if (lastQuery != null && !lastQuery.isEmpty()) {
            filter(lastQuery);
        } else {
            submitList(fullList);
        }
    }

    public void filter(String query) {
        this.lastQuery = query;
        if (query == null || query.isEmpty()) {
            submitList(fullList);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Task> filteredList = new ArrayList<>();
        for (Task task : fullList) {
            boolean matches = false;
            if (task.getPublicId() != null && task.getPublicId().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (task.getContent() != null) {
                if (task.getContent().getName() != null && task.getContent().getName().toLowerCase().contains(lowerQuery)) {
                    matches = true;
                } else if (task.getContent().getDescription() != null && task.getContent().getDescription().toLowerCase().contains(lowerQuery)) {
                    matches = true;
                }
            } else if (task.getVehiclePlate() != null && task.getVehiclePlate().toLowerCase().contains(lowerQuery)) {
                matches = true;
            } else if (task.getVehicle() != null && task.getVehicle().getPlate() != null && task.getVehicle().getPlate().toLowerCase().contains(lowerQuery)) {
                matches = true;
            }

            if (matches) {
                filteredList.add(task);
            }
        }
        submitList(filteredList);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task, listener);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskId;
        private final TextView taskName;
        private final TextView taskDescription;
        private final TextView advancesCount;
        private final TextView taskDate;
        private final TextView statusChip;
        private final TextView vehiclePlate;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskId = itemView.findViewById(R.id.task_id);
            taskName = itemView.findViewById(R.id.task_name);
            taskDescription = itemView.findViewById(R.id.task_description);
            advancesCount = itemView.findViewById(R.id.advances_count);
            taskDate = itemView.findViewById(R.id.task_date);
            statusChip = itemView.findViewById(R.id.status_chip);
            vehiclePlate = itemView.findViewById(R.id.vehicle_plate);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            taskId.setText(task.getPublicId());
            if (task.getContent() != null) {
                taskName.setText(task.getContent().getName());

                String description = task.getContent().getDescription();
                if (description != null && !description.isEmpty()) {
                    taskDescription.setVisibility(View.VISIBLE);
                    taskDescription.setText(description);
                } else {
                    taskDescription.setVisibility(View.GONE);
                }

                String start = DateUtils.formatDateTime(task.getContent().getStartTime());
                String end = DateUtils.formatDateTime(task.getContent().getEndTime());
                String dateRange = start + " - " + end;
                taskDate.setText(dateRange);
            }

            if (task.getAdvancesCount() > 0) {
                advancesCount.setVisibility(View.VISIBLE);
                advancesCount.setText(task.getAdvancesCount() + (task.getAdvancesCount() == 1 ? " avance" : " avances"));
            } else {
                advancesCount.setVisibility(View.GONE);
            }

            if (task.getVehicle() != null && task.getVehicle().getPlate() != null) {
                vehiclePlate.setVisibility(View.VISIBLE);
                vehiclePlate.setText("Vehículo asignado: " + task.getVehicle().getPlate());
            } else if (task.getVehiclePlate() != null && !task.getVehiclePlate().isEmpty()) {
                vehiclePlate.setVisibility(View.VISIBLE);
                vehiclePlate.setText("Vehículo asignado: " + task.getVehiclePlate());
            } else {
                vehiclePlate.setVisibility(View.GONE);
            }

            statusChip.setText(task.getStatus());
            com.example.app_ans.core.ui.StatusUtils.applyStatusColor(statusChip, task.getStatus());

            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}
