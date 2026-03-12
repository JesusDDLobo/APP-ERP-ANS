package com.example.app_ans;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.app_ans.auth.network.AuthApi;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.auth.repository.AuthRepository;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.auth.ui.AuthActivity;
import com.example.app_ans.auth.ui.AuthViewModel;
import com.example.app_ans.auth.ui.AuthViewModelFactory;
import com.example.app_ans.auth.ui.PasswordSetupDialog;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.core.ui.NavbarUtils;
import com.example.app_ans.databinding.ActivityMainBinding;
import com.google.firebase.messaging.FirebaseMessaging;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;
    private AuthSessionManager sessionManager;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> insets);

        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.d("FCM", "POST_NOTIFICATIONS permission granted");
                    } else {
                        Log.w("FCM", "POST_NOTIFICATIONS permission denied");
                    }
                });

        sessionManager = new AuthSessionManager(this);
        AuthRepository repository = new AuthRepository(
                NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        authViewModel = new ViewModelProvider(this, new AuthViewModelFactory(repository))
                .get(AuthViewModel.class);

        setupUi();
        observeViewModel();
        handleIncomingFlags();
        handleTaskRedirect(getIntent());
        requestNotificationPermission();
        registerFCMToken();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingFlags();
        handleTaskRedirect(intent);
    }

    private void handleTaskRedirect(Intent intent) {
        if (intent != null && (intent.hasExtra("TASK_ID") || intent.hasExtra("task_id"))) {
            int taskId = intent.getIntExtra("TASK_ID", intent.getIntExtra("task_id", -1));

            if (taskId == -1) {
                String taskIdStr = intent.getStringExtra("TASK_ID");
                if (taskIdStr == null) taskIdStr = intent.getStringExtra("task_id");

                if (taskIdStr != null) {
                    try {
                        taskId = Integer.parseInt(taskIdStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (taskId != -1) {
                Intent tasksIntent = new Intent(this, TasksActivity.class);
                tasksIntent.putExtra("TASK_ID", taskId);
                tasksIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(tasksIntent);

                intent.removeExtra("TASK_ID");
                intent.removeExtra("task_id");
            }
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Requesting POST_NOTIFICATIONS permission");
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d("FCM", "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    private void setupUi() {
        binding.tasksButton.setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class)));

        binding.hotelButton.setOnClickListener(v ->
                startActivity(new Intent(this, HotelActivity.class)));

        binding.vehicleButton.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.app_ans.vehicles.ui.VehicleDetailActivity.class)));

        NavbarUtils.setupNavbar(
                this,
                null,
                () -> authViewModel.logout(this)
        );
    }

    private void observeViewModel() {
        authViewModel.getError().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getPasswordSetupResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                binding.passwordBanner.setVisibility(android.view.View.GONE);
                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getGoogleAckResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                binding.googleStatusBanner.setVisibility(android.view.View.GONE);
            }
        });
    }

    private void handleIncomingFlags() {
        boolean mustChangePassword = getIntent().getBooleanExtra("must_change_password", false);
        if (mustChangePassword) {
            binding.passwordBanner.setVisibility(android.view.View.VISIBLE);
            binding.passwordBannerButton.setOnClickListener(v -> showPasswordSetupDialog());
        }

        String googleStatus = getIntent().getStringExtra("google_services_status");
        boolean requiresAck = getIntent().getBooleanExtra("google_requires_ack", false);
        if (googleStatus != null && !"ok".equalsIgnoreCase(googleStatus)) {
            binding.googleStatusBanner.setVisibility(android.view.View.VISIBLE);
            binding.googleStatusMessage.setText(
                    getIntent().getStringExtra("google_services_message")
            );
            binding.googleStatusAcknowledge.setVisibility(requiresAck ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.googleStatusAcknowledge.setOnClickListener(v -> authViewModel.acknowledgeGoogleStatus());
        }
    }

    private void showPasswordSetupDialog() {
        PasswordSetupDialog dialog = new PasswordSetupDialog();
        dialog.setListener(password -> authViewModel.setupPassword(password));
        dialog.show(getSupportFragmentManager(), "password_setup");
    }

    private void registerFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null) {
                sendFCMTokenToBackend(token);
            }
        }).addOnFailureListener(e -> Log.e("FCM", "Failed to get FCM token", e));
    }

    private void sendFCMTokenToBackend(String fcmToken) {
        AuthApi authApi = NetworkModule.provideAuthApi(this, sessionManager);

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", fcmToken);
        payload.put("device_name", android.os.Build.MODEL);
        payload.put("device_id", android.os.Build.ID);

        authApi.registerFCMToken(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.i("FCM", "FCM token registered successfully");
                } else {
                    Log.w("FCM", "Failed to register FCM token: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("FCM", "Error registering FCM token", t);
            }
        });
    }
}