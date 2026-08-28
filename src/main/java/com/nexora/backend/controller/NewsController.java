package com.nexora.backend.controller;

import com.nexora.backend.dto.NewsItemResponse;
import com.nexora.backend.dto.NewsPageResponse;
import com.nexora.backend.dto.ReadStatusRequest;
import com.nexora.backend.dto.ReadStatusResponse;
import com.nexora.backend.service.NewsService;
import com.nexora.backend.service.GmailService; // අලුතින් එකතු කළ import එක
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; // අලුතින් එකතු කළ import එක
import java.util.Map;

@RestController
@RequestMapping("/api/admin/news")
public class NewsController {

    private final NewsService newsService;
    private final GmailService gmailService; // අලුතින් එකතු කළ කොටස

    // Constructor එක යාවත්කාලීන කර ඇත
    public NewsController(NewsService newsService, GmailService gmailService) {
        this.newsService = newsService;
        this.gmailService = gmailService;
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

    // --- අලුතින් එකතු කළ Gmail Fetch Endpoint එක ---
    // Fixed: getNewsEmails() returns List<Map<String, String>> (each item has
    // id/sender/subject/snippet, or an "error"/"snippet"-only fallback) — the
    // previous List<String> declaration here didn't match and wouldn't compile.
    @GetMapping("/gmail-fetch")
    public ResponseEntity<List<Map<String, String>>> fetchNewsFromGmail(
            @RequestParam(required = false) List<String> senders) {
        List<Map<String, String>> emails = gmailService.getNewsEmails(senders);
        return ResponseEntity.ok(emails);
    }
}