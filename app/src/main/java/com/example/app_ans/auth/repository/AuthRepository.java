package com.example.app_ans.auth.repository;

import androidx.annotation.Nullable;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.network.AuthApi;
import com.example.app_ans.auth.network.dto.AckRequest;
import com.example.app_ans.auth.network.dto.GoogleCodeRequest;
import com.example.app_ans.auth.network.dto.LoginRequest;
import com.example.app_ans.auth.network.dto.LogoutRequest;
import com.example.app_ans.auth.network.dto.PasswordChangeRequest;
import com.example.app_ans.auth.network.dto.PasswordResetRequest;
import com.example.app_ans.auth.network.dto.PasswordSetupRequest;
import com.example.app_ans.auth.network.dto.SessionResponse;
import com.example.app_ans.auth.session.AuthSessionManager;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/** Coordinates remote calls and local session persistence. */
public class AuthRepository {
    private final AuthApi api;
    private final AuthSessionManager sessionManager;

    public AuthRepository(AuthApi api, AuthSessionManager sessionManager) {
        this.api = api;
        this.sessionManager = sessionManager;
    }

    public UserSession exchangeGoogleCode(String authCode) throws IOException {
        SessionResponse response = execute(api.exchangeGoogleCode(new GoogleCodeRequest(authCode)));
        UserSession session = map(response);
        sessionManager.saveSession(session);
        return session;
    }

    public UserSession login(String email, String password) throws IOException {
        SessionResponse response = execute(api.login(new LoginRequest(email, password)));
        UserSession session = map(response);
        sessionManager.saveSession(session);
        return session;
    }

    public UserSession refreshSession() throws IOException {
        SessionResponse response = execute(api.me());
        UserSession currentSession = sessionManager.restoreSession();
        UserSession refreshedSession = mapFromMe(response, currentSession);
        sessionManager.saveSession(refreshedSession);
        return refreshedSession;
    }

    public void forcePasswordChange(String currentPassword, String newPassword) throws IOException {
        execute(api.forcePasswordChange(new PasswordChangeRequest(currentPassword, newPassword)));
    }

    public void setupPassword(String password) throws IOException {
        execute(api.completePasswordSetup(new PasswordSetupRequest(password)));
    }

    public void forgotPassword(String email) throws IOException {
        execute(api.forgotPassword(new PasswordResetRequest(email)));
    }

    public void acknowledgeGoogleStatus() throws IOException {
        execute(api.status(new AckRequest(true)));
    }

    public void logout() throws IOException {
        String token = sessionManager.getAccessToken();
        try {
            if (token != null) {
                execute(api.logout(new LogoutRequest(token)));
            }
        } finally {
            sessionManager.clear();
        }
    }

    @Nullable
    public String activeAccessToken() {
        return sessionManager.getAccessToken();
    }

    private UserSession map(SessionResponse response) {
        return new UserSession(
                response.getAccessToken(),
                response.getRefreshToken(),
                response.isMustChangePassword(),
                response.getGoogleServicesStatus(),
                response.getGoogleServicesMessage(),
                response.isGoogleStatusRequiresAck(),
                response.resolveRole()
        );
    }

    private UserSession mapFromMe(SessionResponse response, @Nullable UserSession currentSession) {
        return new UserSession(
                currentSession != null ? currentSession.getAccessToken() : null,
                currentSession != null ? currentSession.getRefreshToken() : null,
                response.isMustChangePassword(),
                response.getGoogleServicesStatus() != null
                        ? response.getGoogleServicesStatus()
                        : (currentSession != null ? currentSession.getGoogleServicesStatus() : "ok"),
                response.getGoogleServicesMessage() != null
                        ? response.getGoogleServicesMessage()
                        : (currentSession != null ? currentSession.getGoogleServicesMessage() : null),
                response.isGoogleStatusRequiresAck(),
                response.resolveRole()
        );
    }

    private <T> T execute(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Auth request failed: " + response.code());
        }
        return response.body();
    }
}