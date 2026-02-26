package com.example.app_ans.tasks.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.tasks.persistence.PendingLocationUpdate;
import com.example.app_ans.tasks.persistence.PendingLocationUpdateDao;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;

import java.util.List;

import retrofit2.Response;

public class SyncLocationWorker extends Worker {
    private static final String TAG = "SyncLocationWorker";

    public SyncLocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting location sync...");
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        PendingLocationUpdateDao dao = db.pendingLocationUpdateDao();
        List<PendingLocationUpdate> pendingUpdates = dao.getAllPending();

        if (pendingUpdates.isEmpty()) {
            return Result.success();
        }

        AuthSessionManager sessionManager = new AuthSessionManager(getApplicationContext());
        TaskApi api = NetworkModule.provideTaskApi(getApplicationContext(), sessionManager);

        boolean allSuccessful = true;

        for (PendingLocationUpdate update : pendingUpdates) {
            try {
                Response<Void> response = api.updateTechnicianLocation(
                        update.getTaskId(),
                        update.getLatitude(),
                        update.getLongitude(),
                        update.getTimestamp()
                ).execute();

                if (response.isSuccessful()) {
                    update.setStatus("SYNCED");
                    dao.update(update);
                } else {
                    Log.e(TAG, "Failed to sync location for task " + update.getTaskId() + ": " + response.code());
                    allSuccessful = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing location for task " + update.getTaskId(), e);
                allSuccessful = false;
            }
        }

        dao.clearSynced();

        return allSuccessful ? Result.success() : Result.retry();
    }
}

