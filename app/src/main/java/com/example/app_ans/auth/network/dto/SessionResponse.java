package com.example.app_ans.auth.network.dto;

import com.google.gson.annotations.SerializedName;

public class SessionResponse {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("refresh_token")
    private String refreshToken;
    @SerializedName("must_change_password")
    private boolean mustChangePassword;
    @SerializedName("google_services_status")
    private String googleServicesStatus;
    @SerializedName("google_services_message")
    private String googleServicesMessage;
    @SerializedName("google_status_requires_ack")
    private boolean googleStatusRequiresAck;
    @SerializedName("user")
    private UserData user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getGoogleServicesStatus() {
        return googleServicesStatus;
    }

    public String getGoogleServicesMessage() {
        return googleServicesMessage;
    }

    public boolean isGoogleStatusRequiresAck() {
        return googleStatusRequiresAck;
    }
    
    public UserData getUser() {
        return user;
    }

    public static class UserData {
        @SerializedName("id")
        private int id;
        @SerializedName("email")
        private String email;
        @SerializedName("name")
        private String name;
        @SerializedName("google_access_token")
        private String googleAccessToken;
        
        public String getGoogleAccessToken() { return googleAccessToken; }
    }
}

