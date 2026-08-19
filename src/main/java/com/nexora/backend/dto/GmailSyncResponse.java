package com.nexora.backend.dto;

public class GmailSyncResponse {
    private int imported;
    private int skipped;
    private String lastSyncedAt;

    public GmailSyncResponse(int imported, int skipped, String lastSyncedAt) {
        this.imported = imported;
        this.skipped = skipped;
        this.lastSyncedAt = lastSyncedAt;
    }

    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public String getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(String lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
