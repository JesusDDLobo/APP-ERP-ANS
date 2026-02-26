package com.example.app_ans.vehicles.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app_ans.vehicles.model.Vehicle;
import com.example.app_ans.vehicles.repository.VehicleRepository;

import java.util.Map;
import java.util.List;
import okhttp3.MultipartBody;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VehicleViewModel extends ViewModel {
    private final VehicleRepository repository;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Vehicle> vehicleLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> maintenanceConfigLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> maintenanceSubmittedLiveData = new MutableLiveData<>();

    public VehicleViewModel(VehicleRepository repository) {
        this.repository = repository;
    }

    public LiveData<Vehicle> getVehicle() { return vehicleLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<Map<String, Object>> getMaintenanceConfig() { return maintenanceConfigLiveData; }
    public LiveData<Boolean> getMaintenanceSubmitted() { return maintenanceSubmittedLiveData; }

    public void loadMyVehicle() {
        loadingLiveData.postValue(true);
        errorLiveData.postValue(null);
        executor.execute(() -> {
            try {
                Vehicle vehicle = repository.getMyVehicle();
                vehicleLiveData.postValue(vehicle);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void loadMaintenanceConfig(String type) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                Map<String, Object> config = repository.getMaintenanceConfig(type);
                maintenanceConfigLiveData.postValue(config);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void submitMaintenance(int vehicleId, String type, double odometer, String dataJson, List<MultipartBody.Part> images) {
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                repository.submitMaintenance(vehicleId, type, odometer, dataJson, images);
                maintenanceSubmittedLiveData.postValue(true);
                // Reload vehicle to update status
                loadMyVehicle();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }
}
