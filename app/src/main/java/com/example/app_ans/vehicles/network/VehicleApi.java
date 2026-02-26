package com.example.app_ans.vehicles.network;

import com.example.app_ans.vehicles.model.Vehicle;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.util.Map;
import java.util.List;

public interface VehicleApi {
    @GET("api/vehicles/my-vehicle")
    Call<Vehicle> getMyVehicle();

    @GET("api/maintenance/config/{type}")
    Call<Map<String, Object>> getMaintenanceConfig(@Path("type") String type);

    @Multipart
    @POST("api/maintenance")
    Call<Map<String, Object>> submitMaintenance(
            @Part("vehicle_id") RequestBody vehicleId,
            @Part("type") RequestBody type,
            @Part("odometer") RequestBody odometer,
            @Part("data") RequestBody data,
            @Part List<MultipartBody.Part> images
    );
}
