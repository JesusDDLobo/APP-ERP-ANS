package com.example.app_ans.auth.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.auth.repository.AuthRepository;

public class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final AuthRepository repository;

    public AuthViewModelFactory(AuthRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

