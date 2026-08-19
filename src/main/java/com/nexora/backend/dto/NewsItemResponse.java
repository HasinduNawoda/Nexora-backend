package com.nexora.backend.dto;

public class NewsItemResponse {
    private Long id;
    private String title;
    private String source;
    private String receivedAt;
    private String category;
    private String preview;
    private String sourceUrl;
    private boolean read;

    public NewsItemResponse(Long id, String title, String source, String receivedAt,
                            String category, String preview, String sourceUrl, boolean read) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.receivedAt = receivedAt;
        this.category = category;
        this.preview = preview;
        this.sourceUrl = sourceUrl;
        this.read = read;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
