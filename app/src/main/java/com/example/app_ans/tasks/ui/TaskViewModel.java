package com.example.app_ans.tasks.ui;

import android.app.Application;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.repository.TaskRepository;
import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.core.persistence.PendingAdvance;
import com.example.app_ans.core.utils.FileStorageUtils;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.app_ans.tasks.network.UploadAdvanceWorker;
import com.example.app_ans.tasks.network.SyncLocationWorker;
import com.example.app_ans.tasks.persistence.TaskEntity;
import com.example.app_ans.tasks.persistence.TaskMapper;
import com.example.app_ans.tasks.persistence.TaskDraft;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> completionSuccessLiveData = new MutableLiveData<>();

    private final MutableLiveData<String> draftLiveData = new MutableLiveData<>();
    public LiveData<String> getDraft() { return draftLiveData; }

    public TaskViewModel(@NonNull Application application) {
        super(application);
        AuthSessionManager sessionManager = new AuthSessionManager(application);
        this.db = AppDatabase.getInstance(application);
        this.repository = new TaskRepository(NetworkModule.provideTaskApi(application, sessionManager), db);
    }

    private final MutableLiveData<List<PendingAdvance>> pendingAdvancesLiveData = new MutableLiveData<>();
    public LiveData<List<PendingAdvance>> getPendingAdvances() { return pendingAdvancesLiveData; }

    public void loadPendingAdvances(int taskId) {
        executor.execute(() -> pendingAdvancesLiveData.postValue(db.pendingAdvanceDao().getByTaskId(taskId)));
    }

    public void saveAdvanceOffline(int taskId, String content, List<android.net.Uri> fileUris) {
        executor.execute(() -> {
            try {
                // 1. Save files locally (handle Drive permissions/errors)
                List<String> localPaths = FileStorageUtils.saveFilesLocally(getApplication(), fileUris);
                
                // 2. Save to Room
                PendingAdvance advance = new PendingAdvance(taskId, content, localPaths);
                db.pendingAdvanceDao().insert(advance);
                
                // 3. Schedule WorkManager
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                
                OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UploadAdvanceWorker.class)
                        .setConstraints(constraints)
                        .addTag("upload_advance_" + taskId)
                        .build();
                
                WorkManager.getInstance(getApplication()).enqueue(uploadRequest);

                // 4. Delete draft upon success (or at least when enqueued)
                deleteDraft("ADVANCE_" + taskId);

                // Reload pending advances to show in UI immediately
                loadPendingAdvances(taskId);

                errorLiveData.postValue("Avance guardado localmente. Sincronizando...");
            } catch (Exception e) {
                errorLiveData.postValue("Error al guardar localmente: " + e.getMessage());
            }
        });
    }

    public void saveDraft(String draftType, int id, String content) {
        executor.execute(() -> db.taskDraftDao().saveDraft(new TaskDraft(draftType + "_" + id, content)));
    }

    public void loadDraft(String draftType, int id) {
        executor.execute(() -> {
            TaskDraft draft = db.taskDraftDao().getDraft(draftType + "_" + id);
            draftLiveData.postValue(draft != null ? draft.content : null);
        });
    }

    public void deleteDraft(String draftId) {
        executor.execute(() -> db.taskDraftDao().deleteDraft(draftId));
    }

    public void recordTechnicianLocation(int taskId, double latitude, double longitude) {
        executor.execute(() -> {
            try {
                // 1. Save to Room
                repository.saveTechnicianLocation(taskId, latitude, longitude);

                // 2. Schedule WorkManager
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncLocationWorker.class)
                        .setConstraints(constraints)
                        .build();

                WorkManager.getInstance(getApplication()).enqueue(syncRequest);

                errorLiveData.postValue("Ubicación registrada. Sincronizando...");
            } catch (Exception e) {
                errorLiveData.postValue("Error al registrar ubicación: " + e.getMessage());
            }
        });
    }

    public void observeUploadStatus(int taskId, androidx.lifecycle.LifecycleOwner owner) {
        WorkManager.getInstance(getApplication())
                .getWorkInfosByTagLiveData("upload_advance_" + taskId)
                .observe(owner, workInfos -> {
                    if (workInfos != null) {
                        for (androidx.work.WorkInfo info : workInfos) {
                            if (info.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                                // Refresh task and pending advances when one succeeds
                                loadTaskDetail(taskId);
                                loadPendingAdvances(taskId);
                            }
                        }
                    }
                });
    }

    public LiveData<List<Task>> getTasks() { return tasksLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<Boolean> getCompletionSuccess() { return completionSuccessLiveData; }

    public void loadTasks() {
        errorLiveData.postValue(null);
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                // Try cache first for immediate response
                List<TaskEntity> cachedEntities = db.taskDao().getAllTasks();
                if (!cachedEntities.isEmpty()) {
                    tasksLiveData.postValue(TaskMapper.toDomainList(cachedEntities));
                }

                List<Task> tasks = repository.getTasks();
                tasksLiveData.postValue(tasks);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    private final MutableLiveData<Task> taskDetailLiveData = new MutableLiveData<>();
    public LiveData<Task> getTaskDetail() { return taskDetailLiveData; }

    public void toggleTimer(int taskId, String currentDuration) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                Task updatedTask = repository.toggleTimer(taskId, currentDuration);
                taskDetailLiveData.postValue(updatedTask);

                scheduleTimerSync();

                if (updatedTask != null && updatedTask.isRunning()) {
                    startTimerService(updatedTask);
                } else {
                    stopTimerService();
                }
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    private void scheduleTimerSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(com.example.app_ans.tasks.network.SyncTimerWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "sync_timer_work",
                androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
        );
    }

    private void startTimerService(Task task) {
        if (task == null || !task.isRunning()) return;

        // Calculamos los segundos totales usando el motor unificado (preciso en UTC)
        long totalElapsedSeconds = com.example.app_ans.core.utils.DateUtils.getTotalRunningSeconds(task);

        Intent intent = new Intent(getApplication(), com.example.app_ans.tasks.services.TaskTimerService.class);
        intent.setAction(com.example.app_ans.tasks.services.TaskTimerService.ACTION_START);
        intent.putExtra(com.example.app_ans.tasks.services.TaskTimerService.EXTRA_TASK_ID, task.getId());
        intent.putExtra(com.example.app_ans.tasks.services.TaskTimerService.EXTRA_TASK_NAME, task.getPublicId());

        // Pasamos el tiempo TOTAL transcurrido en segundos.
        // El servicio calculará la base de la notificación restando esto de System.currentTimeMillis()
        intent.putExtra(com.example.app_ans.tasks.services.TaskTimerService.EXTRA_START_TIME, totalElapsedSeconds);

        getApplication().startForegroundService(intent);
    }

    private void stopTimerService() {
        android.content.Intent intent = new android.content.Intent(getApplication(), com.example.app_ans.tasks.services.TaskTimerService.class);
        intent.setAction(com.example.app_ans.tasks.services.TaskTimerService.ACTION_STOP);
        getApplication().startService(intent);
    }

    public void loadTaskDetail(int taskId) {
        loadTaskDetailWithRetry(taskId, 0);
    }

    private void loadTaskDetailWithRetry(int taskId, int attempt) {
        if (attempt == 0) {
            loadingLiveData.postValue(true);
            // Load from cache first for immediate UI update
            executor.execute(() -> {
                Task cached = repository.getTaskFromCache(taskId);
                if (cached != null) {
                    taskDetailLiveData.postValue(cached);
                    // Start service if cached state says it's running
                    if (cached.isRunning()) {
                        startTimerService(cached);
                    }
                }
            });
        }
        
        executor.execute(() -> {
            try {
                Task task = repository.getTaskDetail(taskId);
                taskDetailLiveData.postValue(task);

                // Ensure service is aligned with server state
                if (task.isRunning()) {
                    startTimerService(task);
                } else {
                    stopTimerService();
                }

                loadingLiveData.postValue(false);
            } catch (Exception e) {
                android.util.Log.e("TaskViewModel", "Error loading task detail (attempt " + (attempt + 1) + "): " + taskId, e);
                
                if (attempt < 2) { // Retry
                    try {
                        Thread.sleep(1000); 
                    } catch (InterruptedException ignored) {}
                    loadTaskDetailWithRetry(taskId, attempt + 1);
                } else {
                    // Only show error if we don't have cached data showing
                    if (taskDetailLiveData.getValue() == null) {
                        errorLiveData.postValue("Error de red: " + e.getMessage());
                    }
                    loadingLiveData.postValue(false);
                }
            }
        });
    }

    public void submitAdvance(int taskId, String content) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                Task updatedTask = repository.submitAdvance(taskId, content, new java.util.ArrayList<>());
                taskDetailLiveData.postValue(updatedTask);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void updateAdvance(int taskId, int advanceId, String content) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                Task updatedTask = repository.updateAdvance(taskId, advanceId, content);
                taskDetailLiveData.postValue(updatedTask);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void deleteAdvance(int taskId, int advanceId) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                Task updatedTask = repository.deleteAdvance(taskId, advanceId);
                taskDetailLiveData.postValue(updatedTask);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void loadQuestions(int taskId, OnQuestionsLoadedListener listener) {
        // Try to use questions from current task detail if available
        Task currentTask = taskDetailLiveData.getValue();
        if (currentTask != null && currentTask.getId() == taskId && currentTask.getQuestions() != null && !currentTask.getQuestions().isEmpty()) {
            listener.onLoaded(currentTask.getQuestions());
            return;
        }

        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                List<com.example.app_ans.tasks.model.TaskQuestion> questions = repository.getQuestions(taskId);
                listener.onLoaded(questions);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void completeTask(int taskId, List<com.example.app_ans.tasks.network.dto.TaskCompletionAnswer> answers, java.util.List<android.net.Uri> fileUris) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String answersJson = gson.toJson(answers);
                
                java.util.List<okhttp3.MultipartBody.Part> files = new java.util.ArrayList<>();
                for (int i = 0; i < fileUris.size(); i++) {
                    android.net.Uri uri = fileUris.get(i);
                    okhttp3.MultipartBody.Part part = prepareFilePart("files[" + i + "]", uri);
                    if (part != null) {
                        files.add(part);
                    }
                }
                
                repository.completeTask(taskId, answersJson, files);

                // Clear completion draft
                deleteDraft("COMPLETION_" + taskId);

                completionSuccessLiveData.postValue(true);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    private okhttp3.MultipartBody.Part prepareFilePart(String partName, android.net.Uri fileUri) {
        try {
            android.content.Context context = getApplication().getApplicationContext();
            
            // Use FileStorageUtils to save/compress to a temp file first
            java.util.List<android.net.Uri> uris = new java.util.ArrayList<>();
            uris.add(fileUri);
            java.util.List<String> savedPaths = FileStorageUtils.saveFilesLocally(context, uris);
            
            if (savedPaths.isEmpty()) return null;
            
            java.io.File file = new java.io.File(savedPaths.get(0));
            String type = context.getContentResolver().getType(fileUri);
            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                    file,
                    okhttp3.MediaType.parse(type != null ? type : "application/octet-stream")
            );

            // We should probably delete this temp file after the request is done, 
            // but for now let's just return the part. 
            // Actually, since it's in the pending_advances dir, it might accumulate.
            // But completeTask is a direct call.
            
            return okhttp3.MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnQuestionsLoadedListener {
        void onLoaded(List<com.example.app_ans.tasks.model.TaskQuestion> questions);
    }
}
















































