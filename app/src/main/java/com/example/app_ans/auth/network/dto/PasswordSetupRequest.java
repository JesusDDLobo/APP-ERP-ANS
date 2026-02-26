package com.example.app_ans.auth.network.dto;

public class PasswordSetupRequest {
    private final String password;

    public PasswordSetupRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}

