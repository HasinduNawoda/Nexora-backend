package com.nexora.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "news_item")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String gmailMessageId;

    private String title;

    private String source;

    private Instant receivedAt;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String preview;

    private String sourceUrl;

    private boolean read = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGmailMessageId() { return gmailMessageId; }
    public void setGmailMessageId(String gmailMessageId) { this.gmailMessageId = gmailMessageId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
