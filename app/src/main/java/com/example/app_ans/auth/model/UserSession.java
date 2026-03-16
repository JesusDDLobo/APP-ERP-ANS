package com.example.app_ans.auth.model;

import androidx.annotation.Nullable;

/** Represents the session payload returned by the backend. */
public class UserSession {
    private final String accessToken;
    private final String refreshToken;
    private final boolean mustChangePassword;
    private final String googleServicesStatus;
    private final String googleServicesMessage;
    private final boolean googleStatusRequiresAck;
    private final String role;

    public UserSession(
            String accessToken,
            @Nullable String refreshToken,
            boolean mustChangePassword,
            String googleServicesStatus,
            @Nullable String googleServicesMessage,
            boolean googleStatusRequiresAck,
            @Nullable String role
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = mustChangePassword;
        this.googleServicesStatus = googleServicesStatus;
        this.googleServicesMessage = googleServicesMessage;
        this.googleStatusRequiresAck = googleStatusRequiresAck;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public String getGoogleServicesStatus() {
        return googleServicesStatus;
    }

    @Nullable
    public String getGoogleServicesMessage() {
        return googleServicesMessage;
    }

    public boolean getGoogleStatusRequiresAck() {
        return googleStatusRequiresAck;
    }

    @Nullable
    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role != null && role.trim().equalsIgnoreCase("admin");
    }
}