package com.example.app_ans.vehicles.repository;

import com.example.app_ans.vehicles.model.Vehicle;
import com.example.app_ans.vehicles.network.VehicleApi;

import java.io.IOException;
import java.util.Map;
import java.util.List;

import retrofit2.Response;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.MediaType;

public class VehicleRepository {
    private final VehicleApi api;

    public VehicleRepository(VehicleApi api) {
        this.api = api;
    }

    public Vehicle getMyVehicle() throws IOException {
        Response<Vehicle> response = api.getMyVehicle().execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else if (response.code() == 404) {
            throw new IOException("No hay un vehículo asignado.");
        } else {
            throw new IOException("Error al obtener información del vehículo: " + response.code());
        }
    }

    public Map<String, Object> getMaintenanceConfig(String type) throws IOException {
        Response<Map<String, Object>> response = api.getMaintenanceConfig(type).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            throw new IOException("Error al obtener configuración de mantenimiento.");
        }
    }

    public Map<String, Object> submitMaintenance(int vehicleId, String type, double odometer, String dataJson, List<MultipartBody.Part> images) throws IOException {
        RequestBody vehicleIdPart = RequestBody.create(String.valueOf(vehicleId), MediaType.parse("text/plain"));
        RequestBody typePart = RequestBody.create(type, MediaType.parse("text/plain"));
        RequestBody odometerPart = RequestBody.create(String.valueOf(odometer), MediaType.parse("text/plain"));
        RequestBody dataPart = RequestBody.create(dataJson, MediaType.parse("text/plain"));

        Response<Map<String, Object>> response = api.submitMaintenance(vehicleIdPart, typePart, odometerPart, dataPart, images).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            throw new IOException("Error al enviar mantenimiento: " + response.code());
        }
    }
}
