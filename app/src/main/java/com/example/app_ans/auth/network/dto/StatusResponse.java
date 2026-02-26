package com.example.app_ans.auth.network.dto;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
    @SerializedName("google_services_status")
    private String status;
    @SerializedName("google_services_message")
    private String message;
    @SerializedName("google_status_requires_ack")
    private boolean requiresAck;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRequiresAck() {
        return requiresAck;
    }
}

