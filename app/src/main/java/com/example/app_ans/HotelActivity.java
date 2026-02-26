package com.example.app_ans;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app_ans.databinding.ActivityHotelBinding;
import com.example.app_ans.notifications.NotificationViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HotelActivity extends AppCompatActivity {

    private ActivityHotelBinding binding;
    private NotificationViewModel notificationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHotelBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // Initialize AuthViewModel for logout in Navbar
        com.example.app_ans.auth.session.AuthSessionManager sessionManager = new com.example.app_ans.auth.session.AuthSessionManager(this);
        com.example.app_ans.auth.repository.AuthRepository repository = new com.example.app_ans.auth.repository.AuthRepository(
                com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        com.example.app_ans.auth.ui.AuthViewModel authViewModel = new ViewModelProvider(this, new com.example.app_ans.auth.ui.AuthViewModelFactory(repository))
                .get(com.example.app_ans.auth.ui.AuthViewModel.class);

        // Replicate TasksActivity: handle window insets manually for the navbar
        View navbar = findViewById(R.id.navbar);
        if (navbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(0, statusBarHeight, 0, 0);
                return insets;
            });
        }

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Setup Navbar using utility with logout action
        com.example.app_ans.core.ui.NavbarUtils.setupNavbar(this, "Hotel", this::showNotificationsBottomSheet, () -> authViewModel.logout(this));

        // Observe logout result
        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                android.content.Intent intent = new android.content.Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showNotificationsBottomSheet() {
        com.example.app_ans.notifications.NotificationsBottomSheetFragment bottomSheet =
            new com.example.app_ans.notifications.NotificationsBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "notifications_bottom_sheet");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationViewModel != null) {
            notificationViewModel.updateUnreadCount();
        }
    }
}
