package com.nexora.backend.controller;

import com.nexora.backend.dto.NewsItemResponse;
import com.nexora.backend.dto.NewsPageResponse;
import com.nexora.backend.dto.ReadStatusRequest;
import com.nexora.backend.dto.ReadStatusResponse;
import com.nexora.backend.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<NewsPageResponse> getNewsItems(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(newsService.getNewsItems(query, source, category, read, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsItemResponse> getNewsItem(@PathVariable Long id) {
        NewsItemResponse item = newsService.getNewsItem(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ReadStatusResponse> updateReadStatus(@PathVariable Long id,
                                                                @RequestBody ReadStatusRequest request) {
        ReadStatusResponse response = newsService.updateReadStatus(id, request.isRead());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismissNewsItem(@PathVariable Long id) {
        if (!newsService.dismissNewsItem(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
