package com.example.app_ans;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.auth.ui.AuthActivity;

/** Splash screen that routes to auth or dashboard based on session status. */
public class SplashActivity extends AppCompatActivity {

    private static final long DELAY_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, DELAY_MS);
    }

    private void routeNext() {
        AuthSessionManager sessionManager = new AuthSessionManager(this);
        Intent intent;
        if (sessionManager.hasActiveSession()) {
            intent = new Intent(this, MainActivity.class);
            UserSession session = sessionManager.restoreSession();
            if (session != null) {
                intent.putExtra("must_change_password", session.mustChangePassword());
                intent.putExtra("google_services_status", session.getGoogleServicesStatus());
                intent.putExtra("google_services_message", session.getGoogleServicesMessage());
                intent.putExtra("google_requires_ack", session.getGoogleStatusRequiresAck());
            }

            // Forward ANY other extras (like TASK_ID from notifications)
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
        } else {
            intent = new Intent(this, AuthActivity.class);
        }
        startActivity(intent);
        finish();
    }
}