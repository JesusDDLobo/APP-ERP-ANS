package com.example.app_ans.hotels.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.hotels.repository.HotelRepository;

public class HotelViewModelFactory implements ViewModelProvider.Factory {
    private final HotelRepository repository;

    public HotelViewModelFactory(HotelRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HotelViewModel.class)) {
            return (T) new HotelViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
