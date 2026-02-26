package com.example.app_ans.auth.network.dto;

public class AckRequest {
    private final boolean acknowledged;

    public AckRequest(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }
}

