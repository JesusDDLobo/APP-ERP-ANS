package com.example.app_ans.auth.network.dto;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}

