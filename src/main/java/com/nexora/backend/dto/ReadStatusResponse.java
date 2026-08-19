package com.nexora.backend.dto;

public class ReadStatusResponse {
    private boolean read;

    public ReadStatusResponse(boolean read) {
        this.read = read;
    }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
