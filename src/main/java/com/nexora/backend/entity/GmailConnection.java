package com.nexora.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.Instant;

@Entity
@Table(name = "gmail_connection")
public class GmailConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @JsonIgnore
    private Instant tokenExpiry;

    private BigInteger historyId;

    private Instant lastSyncedAt;

    @JsonIgnore
    private String oauthState;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public void setEncryptedAccessToken(String encryptedAccessToken) { this.encryptedAccessToken = encryptedAccessToken; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public void setEncryptedRefreshToken(String encryptedRefreshToken) { this.encryptedRefreshToken = encryptedRefreshToken; }
    public Instant getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(Instant tokenExpiry) { this.tokenExpiry = tokenExpiry; }
    public BigInteger getHistoryId() { return historyId; }
    public void setHistoryId(BigInteger historyId) { this.historyId = historyId; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getOauthState() { return oauthState; }
    public void setOauthState(String oauthState) { this.oauthState = oauthState; }
}
