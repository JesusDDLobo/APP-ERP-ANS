package com.example.app_ans.auth.network.dto;

public class GoogleCodeRequest {
    private final String authCode;

    public GoogleCodeRequest(String authCode) {
        this.authCode = authCode;
    }

    public String getAuthCode() {
        return authCode;
    }
}

