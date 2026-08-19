package com.nexora.backend.dto;

public class GmailAuthUrlResponse {
    private String authUrl;

    public GmailAuthUrlResponse(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getAuthUrl() { return authUrl; }
    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }
}
