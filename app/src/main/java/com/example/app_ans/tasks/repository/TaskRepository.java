package com.example.app_ans.tasks.repository;

import com.example.app_ans.tasks.model.CoordinatorSubWorkOrder;
import com.example.app_ans.tasks.model.SubWorkOrderTasksResponse;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.model.TaskQuestion;
import com.example.app_ans.tasks.network.TaskApi;

import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.tasks.persistence.TaskEntity;
import com.example.app_ans.tasks.persistence.TaskMapper;
import com.example.app_ans.tasks.persistence.PendingLocationUpdate;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public class TaskRepository {
    private final TaskApi api;
    private final AppDatabase db;

    public TaskRepository(TaskApi api, AppDatabase db) {
        this.api = api;
        this.db = db;
    }

    private <T> T execute(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }
        throw new IOException("Error fetching data: " + response.code() + " - " + response.message());
    }

    public List<Task> getTasks() throws IOException {
        try {
            List<Task> tasks = execute(api.getTasks());
            updateTasksCache(tasks);
            return tasks;
        } catch (IOException e) {
            List<TaskEntity> cached = db.taskDao().getAllTasks();
            if (!cached.isEmpty()) {
                return TaskMapper.toDomainList(cached);
            }
            throw e;
        }
    }

    public List<CoordinatorSubWorkOrder> getCoordinatorSubWorkOrders() throws IOException {
        return execute(api.getCoordinatorSubWorkOrders());
    }

    public SubWorkOrderTasksResponse getSubWorkOrderTasks(int subWorkOrderId) throws IOException {
        return execute(api.getSubWorkOrderTasks(subWorkOrderId));
    }

    private void updateTasksCache(List<Task> tasks) {
        for (Task task : tasks) {
            TaskEntity newEntity = TaskMapper.toEntity(task);
            db.taskDao().insertTask(newEntity);
        }
    }

    private int countJsonItems(String json) {
        if (json == null || json.isEmpty() || json.equals("[]") || json.equals("null")) return 0;
        try {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(json);
            if (element.isJsonArray()) return element.getAsJsonArray().size();
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public Task getTaskFromCache(int taskId) {
        TaskEntity entity = db.taskDao().getTaskById(taskId);
        return entity != null ? TaskMapper.toDomain(entity) : null;
    }

    public Task getTaskDetail(int taskId) throws IOException {
        Task task = execute(api.getTaskDetail(taskId));
        if (task != null) {
            int advancesCount = (task.getAdvances() != null) ? task.getAdvances().size() : 0;
            android.util.Log.d("TaskRepository", "getTaskDetail: Task " + taskId + " received from server with " + advancesCount + " advances");
            db.taskDao().insertTask(TaskMapper.toEntity(task));
        }
        return task;
    }

    public Task submitAdvance(int taskId, String content, List<MultipartBody.Part> files) throws IOException {
        RequestBody contentBody = RequestBody.create(content, MediaType.parse("text/plain"));
        Response<Task> response = api.submitAdvance(taskId, contentBody, files).execute();
        if (response.isSuccessful() && response.body() != null) {
            Task task = response.body();
            db.taskDao().insertTask(TaskMapper.toEntity(task));
            return task;
        }
        throw new IOException("Error submitting advance: " + response.code());
    }

    public Task updateAdvance(int taskId, int advanceId, String content) throws IOException {
        Response<Task> response = api.updateAdvance(advanceId, content).execute();
        if (response.isSuccessful() && response.body() != null) {
            Task task = response.body();
            db.taskDao().insertTask(TaskMapper.toEntity(task));
            return task;
        }
        throw new IOException("Error updating advance: " + response.code());
    }

    public Task deleteAdvance(int taskId, int advanceId) throws IOException {
        Task task = execute(api.deleteAdvance(advanceId));
        if (task != null) {
            db.taskDao().insertTask(TaskMapper.toEntity(task));
        }
        return task;
    }

    public Task toggleTimer(int taskId, String currentDuration) throws Exception {
        Response<Task> response = api.toggleTimer(taskId).execute();
        if (response.isSuccessful()) {
            Task task = response.body();
            if (task != null) {
                db.taskDao().insertTask(TaskMapper.toEntity(task));
            }
            return task;
        }
        throw new Exception("Error al cambiar estado del cronómetro");
    }

    public void saveTechnicianLocation(int taskId, double latitude, double longitude) {
        PendingLocationUpdate update = new PendingLocationUpdate(
                taskId,
                latitude,
                longitude,
                System.currentTimeMillis()
        );
        db.pendingLocationUpdateDao().insert(update);
    }

    public List<TaskQuestion> getQuestions(int taskId) throws IOException {
        Response<List<TaskQuestion>> response = api.getQuestions(taskId).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }
        throw new IOException("Error fetching questions: " + response.code());
    }

    public void completeTask(int taskId, String answersJson, List<MultipartBody.Part> files) throws IOException {
        RequestBody answersBody = RequestBody.create(answersJson, MediaType.parse("application/json"));
        Response<Void> response = api.completeTask(taskId, answersBody, files).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Error completing task: " + response.code());
        }
    }
}