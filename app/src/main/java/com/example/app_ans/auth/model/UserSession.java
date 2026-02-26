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

    public UserSession(
            String accessToken,
            String refreshToken,
            boolean mustChangePassword,
            String googleServicesStatus,
            String googleServicesMessage,
            boolean googleStatusRequiresAck
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = mustChangePassword;
        this.googleServicesStatus = googleServicesStatus;
        this.googleServicesMessage = googleServicesMessage;
        this.googleStatusRequiresAck = googleStatusRequiresAck;
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
}

