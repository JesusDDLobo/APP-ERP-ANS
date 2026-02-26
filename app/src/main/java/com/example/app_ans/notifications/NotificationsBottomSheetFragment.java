package com.example.app_ans.notifications;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.tasks.ui.TaskDetailActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationsBottomSheetFragment extends DialogFragment {
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView notificationCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Position dialog at top of screen instead of bottom
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            
            // Set position to top
            params.y = 0;
            params.gravity = android.view.Gravity.TOP;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            
            window.setAttributes(params);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle window insets to avoid status bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight + 16, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        recyclerView = view.findViewById(R.id.notifications_recycler);
        emptyView = view.findViewById(R.id.empty_view);
        notificationCount = view.findViewById(R.id.notification_count);

        view.findViewById(R.id.btn_close_notifications).setOnClickListener(v -> dismiss());

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Setup RecyclerView with click listener
        adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(AppNotification notification) {
                viewModel.markAsRead(notification.id);

                // Navigate to task if it's a task assignment
                if (notification.relatedTaskId > 0) {
                    Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
                    intent.putExtra("TASK_ID", notification.relatedTaskId);
                    startActivity(intent);
                    dismiss();
                }
            }

            @Override
            public void onDeleteClick(AppNotification notification) {
                viewModel.deleteNotification(notification.id);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Observe notifications
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications == null || notifications.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                notificationCount.setText("Sin notificaciones");
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                notificationCount.setText(notifications.size() + " notificación" + (notifications.size() > 1 ? "es" : ""));
                adapter.setNotifications(notifications);
            }
        });

        // Load notifications
        viewModel.loadNotifications();
    }
}
