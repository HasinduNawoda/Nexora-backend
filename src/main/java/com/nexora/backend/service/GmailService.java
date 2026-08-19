package com.nexora.backend.service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.nexora.backend.dto.GmailAuthUrlResponse;
import com.nexora.backend.dto.GmailStatusResponse;
import com.nexora.backend.dto.GmailSyncResponse;
import com.nexora.backend.entity.GmailConnection;
import com.nexora.backend.entity.NewsItem;
import com.nexora.backend.exception.GmailAccessRevokedException;
import com.nexora.backend.repository.GmailConnectionRepository;
import com.nexora.backend.repository.NewsItemRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

@Service
public class GmailService {

    private static final long SINGLE_ROW_ID = 1L;
    private static final int INITIAL_SYNC_MAX_RESULTS = 50;
    private static final int PREVIEW_MAX_LENGTH = 200;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final GmailConnectionRepository connectionRepository;
    private final NewsItemRepository newsItemRepository;
    private final TokenEncryptionService encryptionService;

    @Value("${gmail.client-id}")
    private String clientId;

    @Value("${gmail.client-secret}")
    private String clientSecret;

    @Value("${gmail.redirect-uri}")
    private String redirectUri;

    @Value("${gmail.frontend-redirect}")
    private String frontendRedirect;

    public GmailService(GmailConnectionRepository connectionRepository,
                        NewsItemRepository newsItemRepository,
                        TokenEncryptionService encryptionService) {
        this.connectionRepository = connectionRepository;
        this.newsItemRepository = newsItemRepository;
        this.encryptionService = encryptionService;
    }

    // ─── Status ──────────────────────────────────────────────────────────

    public GmailStatusResponse getStatus() {
        return connectionRepository.findById(SINGLE_ROW_ID)
                .map(conn -> new GmailStatusResponse(
                        true,
                        conn.getEmail(),
                        conn.getLastSyncedAt() != null ? conn.getLastSyncedAt().toString() : null
                ))
                .orElse(new GmailStatusResponse(false, null, null));
    }

    // ─── Auth URL ────────────────────────────────────────────────────────

    public GmailAuthUrlResponse buildAuthUrl() {
        String state = UUID.randomUUID().toString();

        // Store the state in the connection row (upsert)
        GmailConnection conn = connectionRepository.findById(SINGLE_ROW_ID)
                .orElse(new GmailConnection());
        conn.setOauthState(state);
        if (conn.getId() == null) {
            conn.setId(SINGLE_ROW_ID);
        }
        connectionRepository.save(conn);

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + GmailScopes.GMAIL_READONLY
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + state;

        return new GmailAuthUrlResponse(authUrl);
    }

    // ─── OAuth Callback ──────────────────────────────────────────────────

    public String handleCallback(String code, String state) {
        // Verify state parameter
        GmailConnection conn = connectionRepository.findById(SINGLE_ROW_ID).orElse(null);
        if (conn == null || conn.getOauthState() == null || !conn.getOauthState().equals(state)) {
            return frontendRedirect + "?gmail=error&reason=invalid_state";
        }

        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport, JSON_FACTORY, clientId, clientSecret, code, redirectUri
            ).execute();

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            Long expiresInSeconds = tokenResponse.getExpiresInSeconds();

            // Build Gmail service to fetch user profile
            Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(accessToken);
            Gmail gmail = new Gmail.Builder(transport, JSON_FACTORY, credential)
                    .setApplicationName("Nexora")
                    .build();

            Profile profile = gmail.users().getProfile("me").execute();

            // Store connection with encrypted tokens
            conn.setEmail(profile.getEmailAddress());
            conn.setEncryptedAccessToken(encryptionService.encrypt(accessToken));
            conn.setEncryptedRefreshToken(encryptionService.encrypt(refreshToken));
            conn.setTokenExpiry(Instant.now().plusSeconds(expiresInSeconds != null ? expiresInSeconds : 3600));
            conn.setHistoryId(profile.getHistoryId());
            conn.setOauthState(null); // Clear used state
            connectionRepository.save(conn);

            return frontendRedirect + "?gmail=connected";
        } catch (Exception e) {
            return frontendRedirect + "?gmail=error&reason=token_exchange_failed";
        }
    }

    // ─── Disconnect ──────────────────────────────────────────────────────

    public void disconnect() {
        connectionRepository.findById(SINGLE_ROW_ID).ifPresent(conn -> {
            // Attempt to revoke the token at Google (best-effort)
            try {
                String accessToken = encryptionService.decrypt(conn.getEncryptedAccessToken());
                if (accessToken != null) {
                    HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
                    transport.createRequestFactory()
                            .buildPostRequest(
                                    new com.google.api.client.http.GenericUrl(
                                            "https://oauth2.googleapis.com/revoke?token=" + accessToken),
                                    null)
                            .execute();
                }
            } catch (Exception ignored) {
                // Best-effort revocation — don't fail disconnect if Google is unreachable
            }
            connectionRepository.delete(conn);
        });
    }

    // ─── Sync ────────────────────────────────────────────────────────────

    public GmailSyncResponse sync() {
        GmailConnection conn = connectionRepository.findById(SINGLE_ROW_ID)
                .orElseThrow(() -> new GmailAccessRevokedException("Gmail is not connected — please connect first"));

        try {
            Gmail gmail = buildGmailClient(conn);

            int imported = 0;
            int skipped = 0;

            if (conn.getHistoryId() != null && conn.getLastSyncedAt() != null) {
                // Incremental sync via History API
                SyncResult result = syncViaHistory(gmail, conn);
                imported = result.imported;
                skipped = result.skipped;
            } else {
                // Initial sync — fetch recent messages
                SyncResult result = syncInitial(gmail);
                imported = result.imported;
                skipped = result.skipped;
            }

            // Update historyId and lastSyncedAt
            Profile profile = gmail.users().getProfile("me").execute();
            conn.setHistoryId(profile.getHistoryId());
            conn.setLastSyncedAt(Instant.now());
            connectionRepository.save(conn);

            return new GmailSyncResponse(imported, skipped, conn.getLastSyncedAt().toString());
        } catch (TokenResponseException e) {
            handleTokenError(conn);
            throw new GmailAccessRevokedException("Gmail access was revoked — please reconnect");
        } catch (com.google.api.client.http.HttpResponseException e) {
            if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                handleTokenError(conn);
                throw new GmailAccessRevokedException("Gmail access was revoked — please reconnect");
            }
            throw new RuntimeException("Gmail sync failed: " + e.getMessage(), e);
        } catch (GmailAccessRevokedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gmail sync failed: " + e.getMessage(), e);
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private Gmail buildGmailClient(GmailConnection conn) throws Exception {
        refreshAccessTokenIfNeeded(conn);

        String accessToken = encryptionService.decrypt(conn.getEncryptedAccessToken());
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(accessToken);

        return new Gmail.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName("Nexora")
                .build();
    }

    private void refreshAccessTokenIfNeeded(GmailConnection conn) throws Exception {
        if (conn.getTokenExpiry() != null && Instant.now().isBefore(conn.getTokenExpiry().minusSeconds(60))) {
            return; // Token is still fresh
        }

        String refreshToken = encryptionService.decrypt(conn.getEncryptedRefreshToken());
        if (refreshToken == null) {
            throw new GmailAccessRevokedException("No refresh token available — please reconnect Gmail");
        }

        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport, JSON_FACTORY,
                    "https://oauth2.googleapis.com/token",
                    clientId, clientSecret, refreshToken, ""
            ) {{
                setGrantType("refresh_token");
            }}.execute();

            conn.setEncryptedAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()));
            conn.setTokenExpiry(Instant.now().plusSeconds(
                    tokenResponse.getExpiresInSeconds() != null ? tokenResponse.getExpiresInSeconds() : 3600));
            connectionRepository.save(conn);
        } catch (TokenResponseException e) {
            if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
                throw new GmailAccessRevokedException("Gmail access was revoked — please reconnect");
            }
            throw e;
        }
    }

    private SyncResult syncInitial(Gmail gmail) throws Exception {
        int imported = 0;
        int skipped = 0;

        ListMessagesResponse response = gmail.users().messages().list("me")
                .setMaxResults((long) INITIAL_SYNC_MAX_RESULTS)
                .setLabelIds(List.of("INBOX"))
                .execute();

        List<Message> messages = response.getMessages();
        if (messages == null) {
            return new SyncResult(0, 0);
        }

        for (Message msgRef : messages) {
            if (newsItemRepository.existsByGmailMessageId(msgRef.getId())) {
                skipped++;
                continue;
            }

            Message fullMessage = gmail.users().messages().get("me", msgRef.getId())
                    .setFormat("full")
                    .execute();

            NewsItem item = parseMessage(fullMessage);
            if (item != null) {
                newsItemRepository.save(item);
                imported++;
            } else {
                skipped++;
            }
        }

        return new SyncResult(imported, skipped);
    }

    private SyncResult syncViaHistory(Gmail gmail, GmailConnection conn) throws Exception {
        int imported = 0;
        int skipped = 0;

        try {
            ListHistoryResponse historyResponse = gmail.users().history().list("me")
                    .setStartHistoryId(conn.getHistoryId())
                    .setHistoryTypes(List.of("messageAdded"))
                    .setLabelId("INBOX")
                    .execute();

            List<History> histories = historyResponse.getHistory();
            if (histories == null) {
                return new SyncResult(0, 0);
            }

            for (History history : histories) {
                List<HistoryMessageAdded> added = history.getMessagesAdded();
                if (added == null) continue;

                for (HistoryMessageAdded msg : added) {
                    String msgId = msg.getMessage().getId();
                    if (newsItemRepository.existsByGmailMessageId(msgId)) {
                        skipped++;
                        continue;
                    }

                    Message fullMessage = gmail.users().messages().get("me", msgId)
                            .setFormat("full")
                            .execute();

                    NewsItem item = parseMessage(fullMessage);
                    if (item != null) {
                        newsItemRepository.save(item);
                        imported++;
                    } else {
                        skipped++;
                    }
                }
            }
        } catch (com.google.api.client.http.HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                // historyId is too old — fall back to initial sync
                return syncInitial(gmail);
            }
            throw e;
        }

        return new SyncResult(imported, skipped);
    }

    private NewsItem parseMessage(Message message) {
        if (message == null || message.getPayload() == null) {
            return null;
        }

        MessagePart payload = message.getPayload();
        List<MessagePartHeader> headers = payload.getHeaders();
        if (headers == null) {
            return null;
        }

        String subject = null;
        String from = null;
        String dateStr = null;

        for (MessagePartHeader header : headers) {
            switch (header.getName().toLowerCase()) {
                case "subject" -> subject = header.getValue();
                case "from" -> from = header.getValue();
                case "date" -> dateStr = header.getValue();
            }
        }

        if (subject == null && from == null) {
            return null; // Skip messages with no useful metadata
        }

        // Extract body text
        String bodyHtml = extractBody(payload, "text/html");
        String bodyText = extractBody(payload, "text/plain");

        String previewText = "";
        if (bodyText != null && !bodyText.isBlank()) {
            previewText = bodyText.trim();
        } else if (bodyHtml != null && !bodyHtml.isBlank()) {
            previewText = Jsoup.parse(bodyHtml).text();
        }

        if (previewText.length() > PREVIEW_MAX_LENGTH) {
            previewText = previewText.substring(0, PREVIEW_MAX_LENGTH) + "...";
        }

        // Extract source URL from HTML body
        String sourceUrl = extractSourceUrl(bodyHtml);

        // Parse received date
        Instant receivedAt = parseDate(message.getInternalDate());

        // Parse sender name
        String source = parseSenderName(from);

        NewsItem item = new NewsItem();
        item.setGmailMessageId(message.getId());
        item.setTitle(subject != null ? subject : "(No Subject)");
        item.setSource(source != null ? source : "Unknown");
        item.setReceivedAt(receivedAt);
        item.setCategory(guessCategory(from, subject));
        item.setPreview(previewText);
        item.setSourceUrl(sourceUrl);
        item.setRead(false);

        return item;
    }

    private String extractBody(MessagePart part, String mimeType) {
        if (part.getMimeType() != null && part.getMimeType().equals(mimeType)) {
            if (part.getBody() != null && part.getBody().getData() != null) {
                return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
            }
        }

        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                String result = extractBody(child, mimeType);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private String extractSourceUrl(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        try {
            Document doc = Jsoup.parse(html);
            for (Element link : doc.select("a[href]")) {
                String href = link.attr("abs:href");
                if (href.isEmpty()) {
                    href = link.attr("href");
                }
                if (href != null && !href.isBlank()
                        && href.startsWith("http")
                        && !href.contains("unsubscribe")
                        && !href.contains("mailto:")
                        && !href.contains("list-unsubscribe")) {
                    return href;
                }
            }
        } catch (Exception ignored) {
            // Best-effort extraction
        }
        return null;
    }

    private String parseSenderName(String from) {
        if (from == null) return null;
        // "John Doe <john@example.com>" -> "John Doe"
        int angleBracket = from.indexOf('<');
        if (angleBracket > 0) {
            return from.substring(0, angleBracket).trim().replaceAll("^\"|\"$", "");
        }
        return from.trim();
    }

    private Instant parseDate(Long internalDateMs) {
        if (internalDateMs != null) {
            return Instant.ofEpochMilli(internalDateMs);
        }
        return Instant.now();
    }

    private String guessCategory(String from, String subject) {
        if (from == null && subject == null) return null;

        String lowerFrom = from != null ? from.toLowerCase() : "";
        String lowerSubject = subject != null ? subject.toLowerCase() : "";
        String combined = lowerFrom + " " + lowerSubject;

        // Sender-based matching
        if (lowerFrom.contains("github.com") || lowerFrom.contains("gitlab")) return "Development";
        if (lowerFrom.contains("medium.com") || lowerFrom.contains("dev.to")) return "Tech";
        if (lowerFrom.contains("substack.com")) return "Newsletter";
        if (lowerFrom.contains("producthunt")) return "Product";
        if (lowerFrom.contains("dribbble") || lowerFrom.contains("figma")) return "Design";
        if (lowerFrom.contains("stripe") || lowerFrom.contains("paypal")) return "Finance";
        if (lowerFrom.contains("aws.amazon") || lowerFrom.contains("cloud.google") || lowerFrom.contains("azure")) return "Cloud";

        // Subject keyword matching
        if (combined.contains("security") || combined.contains("vulnerability") || combined.contains("cve")) return "Security";
        if (combined.contains("marketing") || combined.contains("campaign") || combined.contains("seo")) return "Marketing";
        if (combined.contains("design") || combined.contains("ux") || combined.contains("ui")) return "Design";
        if (combined.contains("product") || combined.contains("launch") || combined.contains("release")) return "Product";
        if (combined.contains("engineering") || combined.contains("developer") || combined.contains("code")) return "Tech";
        if (combined.contains("newsletter") || combined.contains("digest") || combined.contains("weekly")) return "Newsletter";
        if (combined.contains("business") || combined.contains("revenue") || combined.contains("growth")) return "Business";

        return null;
    }

    private void handleTokenError(GmailConnection conn) {
        // Clear tokens but keep the row so status shows disconnected state
        conn.setEncryptedAccessToken(null);
        conn.setEncryptedRefreshToken(null);
        conn.setTokenExpiry(null);
        connectionRepository.save(conn);
    }

    // Simple holder for sync result counts
    private static class SyncResult {
        final int imported;
        final int skipped;

        SyncResult(int imported, int skipped) {
            this.imported = imported;
            this.skipped = skipped;
        }
    }
}
