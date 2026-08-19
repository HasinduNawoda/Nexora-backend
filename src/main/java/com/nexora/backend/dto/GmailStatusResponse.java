package com.nexora.backend.dto;

public class GmailStatusResponse {
    private boolean connected;
    private String email;
    private String lastSyncedAt;

    public GmailStatusResponse(boolean connected, String email, String lastSyncedAt) {
        this.connected = connected;
        this.email = email;
        this.lastSyncedAt = lastSyncedAt;
    }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(String lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
