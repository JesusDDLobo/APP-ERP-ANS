package com.example.app_ans.tasks.network;

import com.example.app_ans.tasks.model.CoordinatorSubWorkOrder;
import com.example.app_ans.tasks.model.SubWorkOrderTasksResponse;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.model.TaskQuestion;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface TaskApi {
    @GET("api/mobile/tasks")
    Call<List<Task>> getTasks();

    @GET("api/mobile/tasks/{id}")
    Call<Task> getTaskDetail(@Path("id") int taskId);

    @GET("api/mobile/coordinator/sub-work-orders")
    Call<List<CoordinatorSubWorkOrder>> getCoordinatorSubWorkOrders();

    @GET("api/sub-work-orders/{id}/tasks")
    Call<SubWorkOrderTasksResponse> getSubWorkOrderTasks(@Path("id") int subWorkOrderId);

    @Multipart
    @POST("api/mobile/tasks/{id}/advance")
    Call<Task> submitAdvance(
            @Path("id") int taskId,
            @Part("content") RequestBody content,
            @Part List<MultipartBody.Part> files
    );

    @POST("api/mobile/tasks/advances/{advanceId}/update")
    @FormUrlEncoded
    Call<Task> updateAdvance(
            @Path("advanceId") int advanceId,
            @Field("content") String content
    );

    @POST("api/mobile/tasks/advances/{advanceId}/delete")
    Call<Task> deleteAdvance(@Path("advanceId") int advanceId);

    @GET("api/mobile/tasks/{id}/questions")
    Call<List<TaskQuestion>> getQuestions(@Path("id") int taskId);

    @Multipart
    @POST("api/mobile/tasks/{id}/complete")
    Call<Void> completeTask(
            @Path("id") int taskId,
            @Part("answers") RequestBody answersJson,
            @Part List<MultipartBody.Part> files
    );

    @POST("api/mobile/tasks/{id}/timer/toggle")
    Call<Task> toggleTimer(@Path("id") int taskId);

    @POST("api/mobile/tasks/{id}/technician-location")
    @FormUrlEncoded
    Call<Void> updateTechnicianLocation(
            @Path("id") int taskId,
            @Field("latitude") double latitude,
            @Field("longitude") double longitude,
            @Field("timestamp") long timestamp
    );
}