package com.example.app_ans.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.BuildConfig;
import com.example.app_ans.MainActivity;
import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.repository.AuthRepository;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.databinding.ActivityAuthBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;

import okhttp3.logging.HttpLoggingInterceptor;

/** Authentication screen offering Google-first and fallback login. */
public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity";
    private ActivityAuthBinding binding;
    private AuthViewModel viewModel;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) {
                    showError("Google Sign-In no devolvió datos");
                    Log.w(TAG, "Google Sign-In cancelled: no intent data (code=" + result.getResultCode() + ")");
                    return;
                }
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                    if (account != null && account.getServerAuthCode() != null) {
                        Log.i(TAG, "Google Sign-In successful, exchanging code");
                        viewModel.exchangeGoogleCode(account.getServerAuthCode());
                    } else {
                        showError("No se recibió el código de autenticación");
                        Log.w(TAG, "Google Sign-In returned null account or auth code");
                    }
                } catch (ApiException e) {
                    int statusCode = e.getStatusCode();
                    String readable = GoogleSignInStatusCodes.getStatusCodeString(statusCode);
                    showError("Google Sign-In falló: " + readable);
                    Log.e(TAG, "Google Sign-In failed: status=" + statusCode + " - " + readable, e);
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewModel();
        setupGoogleSignin();
        setupListeners();
        observeViewModel();
    }

    private void setupViewModel() {
        AuthSessionManager sessionManager = new AuthSessionManager(this);
        AuthRepository repository = new AuthRepository(
                NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );
        viewModel = new ViewModelProvider(this, new AuthViewModelFactory(repository))
                .get(AuthViewModel.class);
    }

    private void setupGoogleSignin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(BuildConfig.GOOGLE_SERVER_CLIENT_ID, false)
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        binding.googleButton.setOnClickListener(v -> beginGoogleSignIn());
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString();
            String password = binding.passwordInput.getText().toString();
            viewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            if (loading == null) {
                return;
            }
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getSession().observe(this, this::handleSession);
        viewModel.getError().observe(this, this::showError);
    }

    private void beginGoogleSignIn() {
        if (googleSignInClient == null) {
            showError("Error de configuración de Google");
            Log.e(TAG, "GoogleSignInClient is null");
            return;
        }
        Intent signInIntent = googleSignInClient.getSignInIntent();
        Log.d(TAG, "Launching Google Sign-In intent: " + signInIntent.toString());
        googleLauncher.launch(signInIntent);
    }

    private void handleSession(UserSession session) {
        if (session == null) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("must_change_password", session.mustChangePassword());
        intent.putExtra("google_services_status", session.getGoogleServicesStatus());
        intent.putExtra("google_services_message", session.getGoogleServicesMessage());
        intent.putExtra("google_requires_ack", session.getGoogleStatusRequiresAck());
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        if (message == null) {
            return;
        }
        Log.e(TAG, "UI error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
