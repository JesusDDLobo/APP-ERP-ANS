package com.example.app_ans.auth.session;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.storage.SecurePreferences;

/** Handles persistence and retrieval of the user session. */
public class AuthSessionManager {
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_MUST_CHANGE = "must_change_password";
    private static final String KEY_GOOGLE_STATUS = "google_services_status";
    private static final String KEY_GOOGLE_MESSAGE = "google_services_message";
    private static final String KEY_GOOGLE_ACK = "google_status_requires_ack";

    private static final String DEFAULT_FALSE = "false";
    private static final String DEFAULT_GOOGLE_STATUS = "ok";

    private final SecurePreferences securePreferences;

    public AuthSessionManager(Context context) {
        this.securePreferences = new SecurePreferences(context.getApplicationContext());
    }

    public void saveSession(UserSession session) {
        securePreferences.saveToken(KEY_ACCESS, session.getAccessToken());
        if (session.getRefreshToken() != null) {
            securePreferences.saveToken(KEY_REFRESH, session.getRefreshToken());
        }
        securePreferences.saveToken(KEY_MUST_CHANGE, Boolean.toString(session.mustChangePassword()));
        securePreferences.saveToken(KEY_GOOGLE_STATUS, session.getGoogleServicesStatus());
        if (session.getGoogleServicesMessage() != null) {
            securePreferences.saveToken(KEY_GOOGLE_MESSAGE, session.getGoogleServicesMessage());
        }
        securePreferences.saveToken(KEY_GOOGLE_ACK, Boolean.toString(session.getGoogleStatusRequiresAck()));
    }

    public boolean hasActiveSession() {
        return securePreferences.getToken(KEY_ACCESS) != null;
    }

    @Nullable
    public String getAccessToken() {
        return securePreferences.getToken(KEY_ACCESS);
    }

    public void clear() {
        securePreferences.clear();
    }

    public UserSession restoreSession() {
        String access = securePreferences.getToken(KEY_ACCESS);
        if (access == null) {
            return null;
        }
        String refresh = securePreferences.getToken(KEY_REFRESH);
        boolean mustChange = Boolean.parseBoolean(defaultValue(KEY_MUST_CHANGE, DEFAULT_FALSE));
        String googleStatus = defaultValue(KEY_GOOGLE_STATUS, DEFAULT_GOOGLE_STATUS);
        String googleMessage = securePreferences.getToken(KEY_GOOGLE_MESSAGE);
        boolean requiresAck = Boolean.parseBoolean(defaultValue(KEY_GOOGLE_ACK, DEFAULT_FALSE));
        return new UserSession(access, refresh, mustChange, googleStatus, googleMessage, requiresAck);
    }

    private String defaultValue(String key, String fallback) {
        String value = securePreferences.getToken(key);
        return value == null ? fallback : value;
    }
}
