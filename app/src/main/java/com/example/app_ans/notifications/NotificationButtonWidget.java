package com.example.app_ans.notifications;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.app_ans.R;

/**
 * Atomic component: NotificationButton with badge
 * Extends FrameLayout and uses layout XML
 * Reusable across all Activities
 */
public class NotificationButtonWidget extends FrameLayout {
    private NotificationViewModel viewModel;
    private OnNotificationClickListener listener;
    private int badgeCount = 0;
    private ImageButton iconButton;
    private TextView badgeTextView;

    public interface OnNotificationClickListener {
        void onNotificationClick();
    }

    public NotificationButtonWidget(@NonNull Context context) {
        super(context);
        init(context);
    }

    public NotificationButtonWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NotificationButtonWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            // Inflate layout
            LayoutInflater.from(context).inflate(R.layout.notification_button_widget, this, true);

            // Find views
            iconButton = findViewById(R.id.notification_icon);
            badgeTextView = findViewById(R.id.notification_badge);

            if (iconButton == null || badgeTextView == null) {
                Log.e("NotificationButtonWidget", "Failed to find views in layout");
            }
        } catch (Exception e) {
            Log.e("NotificationButtonWidget", "Error initializing widget: " + e.getMessage(), e);
        }
    }

    /**
     * Setup the widget with ViewModel and click listener
     */
    public void setupWithViewModel(ViewModelStoreOwner viewModelOwner, LifecycleOwner lifecycleOwner,
                                   OnNotificationClickListener clickListener) {
        this.listener = clickListener;

        try {
            // Create ViewModel
            viewModel = new ViewModelProvider(viewModelOwner).get(NotificationViewModel.class);

            // Observe unread count
            viewModel.getUnreadCount().observe(lifecycleOwner, count -> {
                updateBadge(count);
            });

            // Load notifications
            viewModel.updateUnreadCount();

            // Setup click listener
            if (iconButton != null) {
                iconButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNotificationClick();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("NotificationButtonWidget", "Error setting up widget: " + e.getMessage(), e);
        }
    }

    private void updateBadge(int count) {
        badgeCount = count;
        if (badgeTextView != null) {
            if (count > 0) {
                badgeTextView.setText(String.valueOf(count > 99 ? 99 : count));
                badgeTextView.setVisibility(VISIBLE);
            } else {
                badgeTextView.setVisibility(GONE);
            }
        }
    }
}

