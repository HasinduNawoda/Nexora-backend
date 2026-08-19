package com.nexora.backend.controller;

import com.nexora.backend.dto.GmailAuthUrlResponse;
import com.nexora.backend.dto.GmailStatusResponse;
import com.nexora.backend.dto.GmailSyncResponse;
import com.nexora.backend.service.GmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/gmail")
public class GmailController {

    private final GmailService gmailService;

    public GmailController(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @GetMapping("/status")
    public ResponseEntity<GmailStatusResponse> getStatus() {
        return ResponseEntity.ok(gmailService.getStatus());
    }

    @GetMapping("/auth-url")
    public ResponseEntity<GmailAuthUrlResponse> getAuthUrl() {
        return ResponseEntity.ok(gmailService.buildAuthUrl());
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        gmailService.disconnect();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<GmailSyncResponse> sync() {
        return ResponseEntity.ok(gmailService.sync());
    }

    // Google redirects the browser here with ?code=&state= — no JWT header.
    // This endpoint is permitAll() in SecurityConfig.
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("code") String code,
                                                @RequestParam("state") String state) {
        String redirectUrl = gmailService.handleCallback(code, state);
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }
}
