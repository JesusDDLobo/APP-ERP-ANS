package com.example.app_ans.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.tasks.ui.TaskDetailActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class NotificationsBottomSheetFragment extends BottomSheetDialogFragment {

    private NotificationViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView notificationCount;
    private NotificationAdapter adapter;

    public NotificationsBottomSheetFragment() {
        super(R.layout.bottom_sheet_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.notifications_recycler);
        emptyView = view.findViewById(R.id.empty_view);
        notificationCount = view.findViewById(R.id.notification_count);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new NotificationAdapter(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(AppNotification notification) {
                viewModel.markAsRead(notification.id);

                if (notification.relatedTaskId > 0) {
                    Intent intent = new Intent(requireActivity(), TaskDetailActivity.class);
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

        recyclerView.setAdapter(adapter);

        observeViewModel();
        viewModel.loadNotifications();
    }

    private void observeViewModel() {
        viewModel.getNotifications().observe(getViewLifecycleOwner(), this::bindNotifications);
    }

    private void bindNotifications(List<AppNotification> notifications) {
        boolean isEmpty = notifications == null || notifications.isEmpty();

        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);

            if (notificationCount != null) {
                notificationCount.setText("Notificaciones");
            }

            TextView badgeHeader = getView() != null
                    ? getView().findViewById(R.id.notification_badge_header)
                    : null;

            if (badgeHeader != null) {
                badgeHeader.setVisibility(View.GONE);
            }

            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        adapter.setNotifications(notifications);

        if (notificationCount != null) {
            notificationCount.setText("Notificaciones");
        }

        TextView badgeHeader = getView() != null
                ? getView().findViewById(R.id.notification_badge_header)
                : null;

        if (badgeHeader != null) {
            badgeHeader.setVisibility(View.VISIBLE);
            badgeHeader.setText(String.valueOf(notifications.size()));
        }
    }
}