package com.example.app_ans.core.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.core.network.NetworkMonitor;
import com.example.app_ans.notifications.AppNotification;
import com.example.app_ans.notifications.NotificationAdapter;
import com.example.app_ans.notifications.NotificationButtonWidget;
import com.example.app_ans.notifications.NotificationViewModel;
import com.example.app_ans.tasks.ui.TaskDetailActivity;

public class NavbarUtils {

    private static PopupWindow notificationsPopup;

    public static void setupNavbar(AppCompatActivity activity, String title) {
        setupNavbar(activity, title, null);
    }

    public static void setupNavbar(AppCompatActivity activity, String title, Runnable logoutListener) {
        View navbar = activity.findViewById(R.id.navbar);
        if (navbar == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        View backButton = navbar.findViewById(R.id.navbar_back_button);
        if (backButton != null) {
            if (activity.isTaskRoot()) {
                backButton.setVisibility(View.GONE);
            } else {
                backButton.setVisibility(View.VISIBLE);
                backButton.setOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
            }
        }

        TextView titleView = navbar.findViewById(R.id.navbar_title);
        if (titleView != null) {
            if (title != null && !title.trim().isEmpty()) {
                titleView.setVisibility(View.VISIBLE);
                titleView.setText(title);
            } else {
                titleView.setVisibility(View.GONE);
            }
        }

        View logoutButton = navbar.findViewById(R.id.navbar_logout_button);
        if (logoutButton != null) {
            if (logoutListener != null) {
                logoutButton.setVisibility(View.VISIBLE);
                logoutButton.setOnClickListener(v -> logoutListener.run());
            } else {
                logoutButton.setVisibility(View.GONE);
            }
        }

        NotificationButtonWidget notificationButton = navbar.findViewById(R.id.navbar_notification_button);
        if (notificationButton != null) {
            notificationButton.setupWithViewModel(activity, activity, () -> showNotificationsPopup(activity));
        }

        setupNetworkStatus(activity, navbar);
    }

    private static void showNotificationsPopup(AppCompatActivity activity) {
        if (notificationsPopup != null && notificationsPopup.isShowing()) {
            notificationsPopup.dismiss();
            return;
        }

        View popupView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_notifications, null, false);

        RecyclerView recyclerView = popupView.findViewById(R.id.notifications_recycler);
        TextView emptyView = popupView.findViewById(R.id.empty_view);
        TextView notificationCount = popupView.findViewById(R.id.notification_count);
        TextView badgeHeader = popupView.findViewById(R.id.notification_badge_header);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        NotificationViewModel notificationViewModel =
                new ViewModelProvider(activity).get(NotificationViewModel.class);

        NotificationAdapter adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(AppNotification notification) {
                notificationViewModel.markAsRead(notification.id);

                if (notification.relatedTaskId > 0) {
                    Intent intent = new Intent(activity, TaskDetailActivity.class);
                    intent.putExtra("TASK_ID", notification.relatedTaskId);
                    activity.startActivity(intent);
                }

                if (notificationsPopup != null) {
                    notificationsPopup.dismiss();
                }
            }

            @Override
            public void onDeleteClick(AppNotification notification) {
                notificationViewModel.deleteNotification(notification.id);
            }
        });

        recyclerView.setAdapter(adapter);

        notificationViewModel.getNotifications().removeObservers(activity);
        notificationViewModel.getNotifications().observe(activity, notifications -> {
            boolean isEmpty = notifications == null || notifications.isEmpty();

            if (isEmpty) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                notificationCount.setText("Notificaciones");
                if (badgeHeader != null) {
                    badgeHeader.setVisibility(View.GONE);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.setNotifications(notifications);
                notificationCount.setText("Notificaciones");
                if (badgeHeader != null) {
                    badgeHeader.setVisibility(View.VISIBLE);
                    badgeHeader.setText(String.valueOf(notifications.size()));
                }
            }
        });

        notificationViewModel.loadNotifications();

        notificationsPopup = new PopupWindow(
                popupView,
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.82),
                RecyclerView.LayoutParams.WRAP_CONTENT,
                true
        );

        notificationsPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        notificationsPopup.setOutsideTouchable(true);
        notificationsPopup.setElevation(12f);

        dimBackground(activity, 0.30f);
        notificationsPopup.setOnDismissListener(() -> restoreBackground(activity));

        View anchor = activity.findViewById(R.id.navbar);
        if (anchor == null) {
            anchor = activity.findViewById(android.R.id.content);
        }

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(
                        (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.82),
                        View.MeasureSpec.AT_MOST
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int xOff = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.09f);
        int yOff = 8;

        notificationsPopup.showAtLocation(anchor, Gravity.TOP | Gravity.START, xOff, anchor.getBottom() + yOff);
    }

    private static void dimBackground(AppCompatActivity activity, float dimAmount) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) activity.getWindow().getAttributes();
        params.alpha = dimAmount;
        activity.getWindow().setAttributes(params);
    }

    private static void restoreBackground(AppCompatActivity activity) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) activity.getWindow().getAttributes();
        params.alpha = 1.0f;
        activity.getWindow().setAttributes(params);
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
                statusLabel.setTextColor(Color.WHITE);
            } else {
                statusDot.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
                statusLabel.setText(R.string.status_offline);
                statusLabel.setTextColor(Color.WHITE);
            }
        });

        networkMonitor.startMonitoring();

        activity.getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                networkMonitor.stopMonitoring();
            }
        });
    }
}