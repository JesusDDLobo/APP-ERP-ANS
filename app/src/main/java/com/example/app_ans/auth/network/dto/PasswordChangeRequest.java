package com.example.app_ans.auth.network.dto;

public class PasswordChangeRequest {
    private final String currentPassword;
    private final String newPassword;

    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}

