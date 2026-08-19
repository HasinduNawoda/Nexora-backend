package com.nexora.backend.dto;

import java.util.List;

public class NewsPageResponse {
    private List<NewsItemResponse> items;
    private int page;
    private int totalPages;

    public NewsPageResponse(List<NewsItemResponse> items, int page, int totalPages) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
    }

    public List<NewsItemResponse> getItems() { return items; }
    public void setItems(List<NewsItemResponse> items) { this.items = items; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
