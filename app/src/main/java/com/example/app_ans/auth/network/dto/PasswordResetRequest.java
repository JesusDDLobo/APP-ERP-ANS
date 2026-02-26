package com.example.app_ans.auth.network.dto;

public class PasswordResetRequest {
    private final String email;

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

