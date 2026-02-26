package com.example.app_ans.tasks.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.core.persistence.PendingAdvance;
import com.example.app_ans.core.utils.FileStorageUtils;
import com.example.app_ans.tasks.model.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class UploadAdvanceWorker extends Worker {
    private static final String TAG = "UploadAdvanceWorker";

    public UploadAdvanceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        List<PendingAdvance> pendingList = db.pendingAdvanceDao().getAllPending();

        if (pendingList.isEmpty()) return Result.success();

        AuthSessionManager sessionManager = new AuthSessionManager(context);
        TaskApi api = NetworkModule.provideTaskApi(context, sessionManager);

        boolean allSuccess = true;

        for (PendingAdvance advance : pendingList) {
            try {
                advance.setStatus("UPLOADING");
                db.pendingAdvanceDao().update(advance);

                // Prepare Multipart
                RequestBody contentBody = RequestBody.create(advance.getContent(), MediaType.parse("text/plain"));
                List<MultipartBody.Part> fileParts = new ArrayList<>();

                for (String path : advance.getFilePaths()) {
                    File file = new File(path);
                    if (file.exists()) {
                        File uploadFile = FileStorageUtils.getCompressedFile(context, file);
                        RequestBody fileBody = RequestBody.create(uploadFile, MediaType.parse("application/octet-stream"));
                        // The backend expects files[]
                        fileParts.add(MultipartBody.Part.createFormData("files[]", uploadFile.getName(), fileBody));
                    }
                }

                Response<Task> response = api.submitAdvance(advance.getTaskId(), contentBody, fileParts).execute();

                if (response.isSuccessful()) {
                    // Update cache with the returned task
                    Task updatedTask = response.body();
                    if (updatedTask != null) {
                        db.taskDao().insertTask(com.example.app_ans.tasks.persistence.TaskMapper.toEntity(updatedTask));
                    }

                    // Cleanup files
                    for (String path : advance.getFilePaths()) {
                        FileStorageUtils.deleteFile(path);
                    }
                    db.pendingAdvanceDao().delete(advance);
                } else {
                    Log.e(TAG, "Failed to upload advance " + advance.getId() + ": " + response.code());
                    if (response.code() == 413) {
                        advance.setStatus("ERROR_TOO_LARGE");
                    } else {
                        advance.setStatus("ERROR");
                    }
                    db.pendingAdvanceDao().update(advance);
                    // If it's 413, don't trigger a global retry for this specific one,
                    // but we still want to retry other potential errors.
                    if (response.code() != 413) {
                        allSuccess = false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in worker for advance " + advance.getId(), e);
                advance.setStatus("ERROR");
                db.pendingAdvanceDao().update(advance);
                allSuccess = false;
            }
        }

        return allSuccess ? Result.success() : Result.retry();
    }
}
