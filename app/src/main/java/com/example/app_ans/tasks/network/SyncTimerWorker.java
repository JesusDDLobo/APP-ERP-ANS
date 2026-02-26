package com.example.app_ans.tasks.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.core.persistence.PendingTimerAction;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.persistence.TaskMapper;

import java.util.List;

import retrofit2.Response;

public class SyncTimerWorker extends Worker {
    public SyncTimerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        AuthSessionManager sessionManager = new AuthSessionManager(getApplicationContext());
        TaskApi api = NetworkModule.provideTaskApi(getApplicationContext(), sessionManager);

        List<PendingTimerAction> pendingActions = db.pendingTimerActionDao().getAllPending();
        if (pendingActions.isEmpty()) {
            return Result.success();
        }

        boolean allSuccess = true;
        for (PendingTimerAction action : pendingActions) {
            try {
                // Sincronización del toggle con el servidor
                Response<Task> response = api.toggleTimer(action.taskId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    Task serverTask = response.body();
                    db.taskDao().insertTask(TaskMapper.toEntity(serverTask));
                    db.pendingTimerActionDao().deleteByTaskId(action.taskId);
                } else {
                    allSuccess = false;
                }
            } catch (Exception e) {
                allSuccess = false;
            }
        }

        return allSuccess ? Result.success() : Result.retry();
    }
}
