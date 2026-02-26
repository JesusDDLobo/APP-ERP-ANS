package com.example.app_ans.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.core.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<AppNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
        void onDeleteClick(AppNotification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<AppNotification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView messageText;
        private final TextView timeText;
        private final ImageButton deleteBtn;
        private final View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.notification_title);
            messageText = itemView.findViewById(R.id.notification_message);
            timeText = itemView.findViewById(R.id.notification_time);
            deleteBtn = itemView.findViewById(R.id.btn_delete_notification);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }

        public void bind(AppNotification notification, OnNotificationClickListener listener) {
            titleText.setText(notification.title);
            messageText.setText(notification.message);
            
            // Convert timestamp (long) to formatted string
            long timestampMs = notification.timestamp;
            String formattedTime = DateUtils.formatDateTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(timestampMs)));
            timeText.setText(formattedTime);

            // Show unread indicator
            unreadIndicator.setVisibility(notification.isRead ? View.GONE : View.VISIBLE);

            // Click to open notification
            itemView.setOnClickListener(v -> listener.onNotificationClick(notification));

            // Delete button
            deleteBtn.setOnClickListener(v -> listener.onDeleteClick(notification));
        }
    }
}
