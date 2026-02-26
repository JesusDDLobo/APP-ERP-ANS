package com.example.app_ans.auth.network.dto;

import com.google.gson.annotations.SerializedName;

public class LogoutRequest {
    @SerializedName("access_token")
    private final String accessToken;

    public LogoutRequest(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}

