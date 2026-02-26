package com.example.app_ans.tasks.persistence;

import com.example.app_ans.tasks.model.Task;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class TaskMapper {
    private static final Gson gson = new Gson();

    public static TaskEntity toEntity(Task task) {
        TaskEntity entity = new TaskEntity();
        entity.id = task.getId();
        entity.publicId = task.getPublicId();
        entity.createdAt = task.getCreatedAt();
        
        entity.stateJson = gson.toJson(task.getState());
        entity.contentJson = gson.toJson(task.getContent());
        entity.assignedUsersJson = gson.toJson(task.getAssignedUsers());
        entity.advancesJson = gson.toJson(task.getAdvances());
        entity.previousTicketJson = gson.toJson(task.getPreviousTicket());
        entity.questionsJson = gson.toJson(task.getQuestions());
        entity.timeLogsJson = gson.toJson(task.getTimeLogs());
        entity.vehicleJson = gson.toJson(task.getVehicle());
        entity.vehiclePlate = task.getVehiclePlate();
        entity.isRunning = task.isRunning();
        entity.totalTimeSpent = task.getTotalTimeSpent();

        return entity;
    }

    public static Task toDomain(TaskEntity entity) {
        if (entity == null) return null;
        
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("id", entity.id);
            json.addProperty("public_id", entity.publicId);
            json.addProperty("created_at", entity.createdAt);
            json.addProperty("is_running", entity.isRunning);
            json.addProperty("total_time_spent", entity.totalTimeSpent);

            if (entity.stateJson != null && !entity.stateJson.equals("null")) json.add("state", gson.fromJson(entity.stateJson, com.google.gson.JsonElement.class));
            if (entity.contentJson != null && !entity.contentJson.equals("null")) json.add("content", gson.fromJson(entity.contentJson, com.google.gson.JsonElement.class));
            if (entity.assignedUsersJson != null && !entity.assignedUsersJson.equals("null")) json.add("assigned_users", gson.fromJson(entity.assignedUsersJson, com.google.gson.JsonElement.class));
            if (entity.advancesJson != null && !entity.advancesJson.equals("null")) json.add("advances", gson.fromJson(entity.advancesJson, com.google.gson.JsonElement.class));
            if (entity.previousTicketJson != null && !entity.previousTicketJson.equals("null")) json.add("previous_ticket", gson.fromJson(entity.previousTicketJson, com.google.gson.JsonElement.class));
            if (entity.questionsJson != null && !entity.questionsJson.equals("null")) json.add("questions", gson.fromJson(entity.questionsJson, com.google.gson.JsonElement.class));
            if (entity.timeLogsJson != null && !entity.timeLogsJson.equals("null")) json.add("time_logs", gson.fromJson(entity.timeLogsJson, com.google.gson.JsonElement.class));
            if (entity.vehicleJson != null && !entity.vehicleJson.equals("null")) json.add("vehicle", gson.fromJson(entity.vehicleJson, com.google.gson.JsonElement.class));
            if (entity.vehiclePlate != null) json.addProperty("vehicle_plate", entity.vehiclePlate);

            Task task = gson.fromJson(json, Task.class);
            if (task != null && task.getAdvances() != null) {
                android.util.Log.d("TaskMapper", "Task " + task.getId() + " mapped with " + task.getAdvances().size() + " advances from cache/entity");
            }
            return task;
        } catch (Exception e) {
            android.util.Log.e("TaskMapper", "Error mapping task " + entity.id, e);
            return null;
        }
    }


    public static List<Task> toDomainList(List<TaskEntity> entities) {
        List<Task> tasks = new ArrayList<>();
        for (TaskEntity entity : entities) {
            tasks.add(toDomain(entity));
        }
        return tasks;
    }
}
