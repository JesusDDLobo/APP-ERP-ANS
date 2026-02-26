package com.example.app_ans.vehicles.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.vehicles.repository.VehicleRepository;

public class VehicleViewModelFactory implements ViewModelProvider.Factory {
    private final VehicleRepository repository;

    public VehicleViewModelFactory(VehicleRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(VehicleViewModel.class)) {
            return (T) new VehicleViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

