package com.example.app_ans.core.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import com.example.app_ans.R;
import com.example.app_ans.core.network.NetworkMonitor;
import com.example.app_ans.notifications.NotificationButtonWidget;

public class NavbarUtils {

    public static void setupNavbar(AppCompatActivity activity, String title, NotificationButtonWidget.OnNotificationClickListener notificationListener) {
        setupNavbar(activity, title, notificationListener, null);
    }

    public static void setupNavbar(AppCompatActivity activity, String title, NotificationButtonWidget.OnNotificationClickListener notificationListener, Runnable logoutListener) {
        View navbar = activity.findViewById(R.id.navbar);
        if (navbar == null) return;

        // Apply window insets for status bar
        ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        // Setup back button
        View backButton = navbar.findViewById(R.id.navbar_back_button);
        if (backButton != null) {
            if (activity.isTaskRoot()) {
                backButton.setVisibility(View.GONE);
            } else {
                backButton.setVisibility(View.VISIBLE);
                backButton.setOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
            }
        }

        // Setup title
        TextView titleView = navbar.findViewById(R.id.navbar_title);
        if (titleView != null && title != null) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(title);
        }

        // Setup Logout Button
        View logoutButton = navbar.findViewById(R.id.navbar_logout_button);
        if (logoutButton != null) {
            if (logoutListener != null) {
                logoutButton.setVisibility(View.VISIBLE);
                logoutButton.setOnClickListener(v -> logoutListener.run());
            } else {
                logoutButton.setVisibility(View.GONE);
            }
        }

        // Setup Notification Button
        NotificationButtonWidget notificationButton = navbar.findViewById(R.id.navbar_notification_button);
        if (notificationButton != null) {
            notificationButton.setupWithViewModel(activity, activity, notificationListener);
        }

        // Setup Network Status
        setupNetworkStatus(activity, navbar);
    }

    private static void setupNetworkStatus(AppCompatActivity activity, View navbar) {
        View statusDot = navbar.findViewById(R.id.status_dot);
        TextView statusLabel = navbar.findViewById(R.id.status_label);

        if (statusDot == null || statusLabel == null) return;

        NetworkMonitor networkMonitor = new NetworkMonitor(activity);
        networkMonitor.isConnected.observe(activity, connected -> {
            boolean isOnline = Boolean.TRUE.equals(connected);
            if (isOnline) {
                statusDot.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                statusLabel.setText(R.string.status_online);
                statusLabel.setTextColor(activity.getResources().getColor(R.color.ans_primary_dark, null));
            } else {
                statusDot.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
                statusLabel.setText(R.string.status_offline);
                statusLabel.setTextColor(Color.GRAY);
            }
        });

        // Start monitoring
        networkMonitor.startMonitoring();

        // Ensure monitoring stops when activity is destroyed to avoid leaks
        activity.getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                networkMonitor.stopMonitoring();
            }
        });
    }
}
