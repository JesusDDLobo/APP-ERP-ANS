package com.example.app_ans.auth.ui;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.repository.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.example.app_ans.BuildConfig;

/** ViewModel that orchestrates login flows without Kotlin coroutines. */
public class AuthViewModel extends ViewModel {
    private final AuthRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<UserSession> sessionLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> logoutLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> passwordSetupLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> googleAckLiveData = new MutableLiveData<>();

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public LiveData<UserSession> getSession() {
        return sessionLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<Boolean> getLogoutResult() {
        return logoutLiveData;
    }

    public LiveData<Boolean> getPasswordSetupResult() {
        return passwordSetupLiveData;
    }

    public LiveData<Boolean> getGoogleAckResult() {
        return googleAckLiveData;
    }

    public void login(String email, String password) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                UserSession session = repository.login(email, password);
                sessionLiveData.postValue(session);
            } catch (Exception e) {
                errorLiveData.postValue("Error de autenticación");
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void exchangeGoogleCode(String authCode) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                UserSession session = repository.exchangeGoogleCode(authCode);
                sessionLiveData.postValue(session);
            } catch (Exception e) {
                errorLiveData.postValue("No se pudo autenticar con Google");
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void logout(Context context) {
        // 1. Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context, gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d("AuthViewModel", "Google Sign-Out complete. Clearing local session.");
            // 2. Clear local session data from repository in background
            executor.execute(() -> {
                try {
                    repository.logout();
                } catch (Exception e) {
                    Log.e("AuthViewModel", "Error during repository logout", e);
                } finally {
                    // 3. Notify UI that logout is complete
                    logoutLiveData.postValue(true);
                }
            });
        });
    }

    public void setupPassword(String password) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                repository.setupPassword(password);
                passwordSetupLiveData.postValue(true);
            } catch (Exception e) {
                errorLiveData.postValue("No se pudo actualizar la contraseña");
                passwordSetupLiveData.postValue(false);
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void acknowledgeGoogleStatus() {
        executor.execute(() -> {
            try {
                repository.acknowledgeGoogleStatus();
                googleAckLiveData.postValue(true);
            } catch (Exception e) {
                errorLiveData.postValue("No se pudo registrar la confirmación");
                googleAckLiveData.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
