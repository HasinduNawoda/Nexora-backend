package com.nexora.backend.controller;

import com.nexora.backend.dto.GmailAuthUrlResponse;
import com.nexora.backend.dto.GmailStatusResponse;
import com.nexora.backend.dto.GmailSyncResponse;
import com.nexora.backend.service.GmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // Google redirects the browser to the FRONTEND /admin/gmail/callback, which
    // then calls this endpoint directly with code+state via the JWT-authenticated
    // api client. Returns JSON so the frontend can handle success/error display.
    // Still permitAll() in SecurityConfig because the frontend page calls it
    // without a JWT (the user may not be "logged in" in the new tab context).
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        boolean success = gmailService.handleCallback(code, state);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid state or token exchange failed"));
        }
    }
}
