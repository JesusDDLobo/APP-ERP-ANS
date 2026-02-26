package com.example.app_ans;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class AppSettings extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
